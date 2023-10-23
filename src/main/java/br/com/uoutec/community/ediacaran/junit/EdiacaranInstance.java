package br.com.uoutec.community.ediacaran.junit;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import br.com.uoutec.community.ediacaran.EdiacaranBootstrap;
import br.com.uoutec.community.ediacaran.PluginManager;
import br.com.uoutec.community.ediacaran.plugins.PluginInitializer;
import br.com.uoutec.community.ediacaran.test.mock.MockBeanDiscover;
import br.com.uoutec.io.resource.DefaultResourceLoader;
import br.com.uoutec.io.resource.Resource;
import br.com.uoutec.io.resource.ResourceLoader;

public class EdiacaranInstance {

	private EdiacaranBootstrap ediacaranBootstrap;
	
	private PluginManager pluginManager;
	
	public void destroy() {
		if(ediacaranBootstrap != null) {
			ediacaranBootstrap.stopApplication();
		}
	}
	
	public void start(Class<?> testClass) throws Throwable{
		
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		
		String config = getConfigPath(testClass);
		
		InputStream in = null;
		try {
			Resource resource = resourceLoader.getResource(config);
			
			if(resource == null) {
				throw new RuntimeException("not found: " + config);
			}
			
			in = resource.getInputStream();
			XMLDecoder xml          = new XMLDecoder(in);
			this.ediacaranBootstrap = (EdiacaranBootstrap) xml.readObject();
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
		}
		
		Map<String,Object> params = getParameters(testClass);
		
		applyDefaultConfiguration(params);

		for(Entry<Class<?>, Object> e: getMocks(testClass).entrySet()) {
			ediacaranBootstrap.addEntity(e.getValue(), e.getKey());	
		}
		
		ediacaranBootstrap.loadApplication(params);
		ediacaranBootstrap.startApplication();
		
		this.pluginManager = ediacaranBootstrap.getPluginManager();
	}
	
	private Map<Class<?>, Object> getMocks(Class<?> testClass) {
		String pluginContext = getPluginContext(testClass);
		
		MockBeanDiscover mbd = new MockBeanDiscover();
		return mbd.getMocks(testClass, pluginContext);
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
		 
    	ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    	
		if(contextVars != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
    	
    	try {
    		return value.call();
    	}
    	finally {
    		if(contextVars != null) {
    			Thread.currentThread().setContextClassLoader(oldClassLoader);
    		}
    	}
    	
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
						"ediacaran" + File.separator + 
						"config" + File.separator +
						"ediacaran-config.xml"
					);
			}
			
			if(!contextParams.containsKey(EdiacaranBootstrap.CONFIG_FILE_VAR)) {
				contextParams.put(
					EdiacaranBootstrap.CONFIG_FILE_VAR, 
					new File(System.getProperty("user.dir") + File.separator + 
					"ediacaran" + File.separator + 
					"config" + File.separator +
					"ediacaran-dev.properties").toURI().toURL().toExternalForm()
				);
			}
			
			if(!contextParams.containsKey(EdiacaranBootstrap.LOGGER_CONFIG_FILE_VAR)) {
				contextParams.put(
					EdiacaranBootstrap.LOGGER_CONFIG_FILE_VAR, 
					new File(System.getProperty("user.dir") + File.separator + 
					"ediacaran" + File.separator + 
					"config" + File.separator +
					"log4j.configuration").toURI().toURL().toExternalForm()
				);
			}
		
			if(!contextParams.containsKey(EdiacaranBootstrap.BASE_PATH_PROPERTY)) {
				contextParams.put(
					EdiacaranBootstrap.BASE_PATH_PROPERTY, 
					"ediacaran" + File.separator 
				);
			}
				
	}
	
	private Map<String,Object> getPluginConfigVars(PluginManager pluginManager, String context){
		return context == null? null : pluginManager.getPluginConfigVars(context);
	}

}
