package br.com.uoutec.ediacaran.junit;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import br.com.uoutec.application.SystemProperties;
import br.com.uoutec.application.io.Path;
import br.com.uoutec.application.io.Vfs;
import br.com.uoutec.application.security.SecurityThread;
import br.com.uoutec.community.ediacaran.test.mock.MockBeanDiscover;
import br.com.uoutec.community.ediacaran.test.mock.SecurityPolicyManagerMock;
import br.com.uoutec.ediacaran.core.EdiacaranBootstrap;
import br.com.uoutec.ediacaran.core.PluginManager;
import br.com.uoutec.ediacaran.core.plugins.PluginException;
import br.com.uoutec.ediacaran.core.plugins.PluginInitializer;
import br.com.uoutec.io.resource.DefaultResourceLoader;
import br.com.uoutec.io.resource.Resource;
import br.com.uoutec.io.resource.ResourceLoader;

public class EdiacaranInstance {

	private EdiacaranBootstrap ediacaranBootstrap;
	
	private PluginManager pluginManager;
	
	private Path[] bases;
	
	private Class<?> testClass;
	
	private Map<String,Object> params;
	
	public EdiacaranInstance() {
		this.bases = new Path[] {
				Vfs.getPath(SystemProperties.getProperty("user.dir")).getPath("ediacaran"),
				Vfs.getPath("file:///develop/ediacaran"),
				Vfs.getPath("file:///ediacaran"),
		};
	}
	
	public void destroy() {
		if(ediacaranBootstrap != null) {
			ediacaranBootstrap.stopApplication();
		}
	}
	
	public void start(Class<?> testClass) throws Throwable{
		
		this.testClass = testClass;
		
		createApplication();
		loadConfiguration();		
		registerMocks();
		startApplication();
		
	}
	
	private void startApplication() {
		ediacaranBootstrap.loadApplication(params);
		ediacaranBootstrap.startApplication();
		
		this.pluginManager = (PluginManager)ediacaranBootstrap.getPluginManager();
	}
	
	private void loadConfiguration() throws MalformedURLException {
		this.params = getParameters(testClass);
		applyDefaultConfiguration(params);
	}
	
	private void registerMocks() {
		String pluginContext = getPluginContext(testClass);
		
		MockBeanDiscover mbd = new MockBeanDiscover();
		
		Map<Class<?>, Object> mocks =  mbd.getMocks(testClass, pluginContext);
		
		for(Entry<Class<?>, Object> e: mocks.entrySet()) {
			ediacaranBootstrap.addEntity(e.getValue(), e.getKey());	
		}
		
		ediacaranBootstrap.setSecurityPolicyManager(new SecurityPolicyManagerMock(pluginContext, testClass));
	}
	
	private void createApplication() {
		
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		
		String config = getConfigPath(testClass);
		
		InputStream in = null;
		ClassLoader old = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(testClass.getClassLoader());
		try {
			Resource resource = resourceLoader.getResource(config);
			
			if(resource == null) {
				throw new RuntimeException("not found: " + config);
			}
			
			in = resource.getInputStream();
			
			ExceptionListener ex = (e)->{
				throw new PluginException(e);
			};
			
			XMLDecoder xml = new XMLDecoder(in, null, ex);
			//XMLDecoder xml = new XMLDecoder(in);
			this.ediacaranBootstrap = (EdiacaranBootstrap)xml.readObject();
			xml.close();
		}
		catch(IOException ex) {
			throw new RuntimeException(ex);
		}
		finally {
			try {
				if(in != null) 
					in.close();
			}
			catch(Throwable ex) {
			}
			Thread.currentThread().setContextClassLoader(old);
		}
		
	}
	
	private String getPluginContext(Class<?> testClass) {
		
		PluginContext pc = testClass.getAnnotation(PluginContext.class);
		if(pc != null && !pc.value().isEmpty()) {
			return pc.value();
		}
		
		return null;
	}
	
	public Object execute(Callable<Object> value, String context) throws Throwable {
	
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, context);
		ClassLoader classLoader = null;
		
		if(contextVars != null) {
			classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
		}
		 
		Throwable[] x = new Throwable[1];
		Object[] r = new Object[1];
		boolean[] dispatched = new boolean[1];
		
		Thread thread = new SecurityThread(()->{
			synchronized(r) {
				try {
					dispatched[0] = false;
					r[0] = value.call(); 
				}
				catch(Throwable ex) {
					x[0] = ex;
				}
				finally {
					dispatched[0] = true;
					r.notifyAll();
				}
			}
		});
		
		thread.setContextClassLoader(classLoader);
		
		try {
			synchronized (r) {
				thread.start();
					while(!dispatched[0]) {
						r.wait();
					}
			}
		}
		catch (InterruptedException e1) {
		}
		
		if(x[0] != null) {
			throw x[0];
		}
		
		return r[0];
	}
	
	private String getConfigPath(Class<?> testClass) {
		ApplicationConfigTest config = testClass.getDeclaredAnnotation(ApplicationConfigTest.class);
		return config == null? "ediacaran/config/ediacaran-config.xml" : config.value();
	}

	private Map<String,Object> getParameters(Class<?> testClass) {
		
		Map<String,Object> r = new HashMap<String,Object>();
		
		ApplicationConfigParametersTest params = testClass.getDeclaredAnnotation(ApplicationConfigParametersTest.class);
		
		if(params != null) {
			for(ApplicationConfigParameterTest p: params.value()) {
				r.put(p.paramName(), p.paramValue());
			}
		}
		
		ApplicationConfigParameterTest param = testClass.getDeclaredAnnotation(ApplicationConfigParameterTest.class);

		if(param != null) {
			r.put(param.paramName(), param.paramValue());
		}
		
		return r;
	}

	private void applyDefaultConfiguration(Map<String, Object> contextParams) throws MalformedURLException {
		
			if(!contextParams.containsKey("app")) {
					contextParams.put(
						"app",
						getPath("config" + Vfs.getSeparator() +	"ediacaran-config.xml")
					);
			}
			
			if(!contextParams.containsKey(EdiacaranBootstrap.CONFIG_FILE_VAR)) {
				contextParams.put(
					EdiacaranBootstrap.CONFIG_FILE_VAR, 
					getPath("config" + Vfs.getSeparator() +	"ediacaran-dev.properties").toURL().toExternalForm()
				);
			}
			
			if(!contextParams.containsKey(EdiacaranBootstrap.LOGGER_CONFIG_FILE_VAR)) {
				contextParams.put(
					EdiacaranBootstrap.LOGGER_CONFIG_FILE_VAR,
					getPath("config" + Vfs.getSeparator() +	"log4j.configuration").toURL().toExternalForm()
				);
			}

			if(!contextParams.containsKey(EdiacaranBootstrap.CONFIG_PATH_PROPERTY)) {
				contextParams.put(
					EdiacaranBootstrap.CONFIG_PATH_PROPERTY,
					getPath("config")
				);
			}
			
			if(!contextParams.containsKey(EdiacaranBootstrap.BASE_PATH_PROPERTY)) {
				contextParams.put(
					EdiacaranBootstrap.BASE_PATH_PROPERTY, 
					getPath(null)
 				);
			}
				
	}
	
	private Path getPath(String path) {
		
		for(Path base: bases) {
			Path p = path == null? base : base.getPath(path);
			if(p.exists()) {
				return p;
			}
		}
		
		throw new RuntimeException(path);
	}
	
	private Map<String,Object> getPluginConfigVars(PluginManager pluginManager, String context){
		return context == null? null : pluginManager.getPluginConfigVars(context);
	}

}
