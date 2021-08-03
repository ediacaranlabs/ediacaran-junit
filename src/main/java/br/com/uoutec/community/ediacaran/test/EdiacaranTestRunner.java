package br.com.uoutec.community.ediacaran.test;

import java.beans.XMLDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import br.com.uoutec.community.ediacaran.EdiacaranBootstrap;
import br.com.uoutec.community.ediacaran.PluginManager;
import br.com.uoutec.community.ediacaran.plugins.PluginInitializer;
import br.com.uoutec.io.resource.DefaultResourceLoader;
import br.com.uoutec.io.resource.Resource;
import br.com.uoutec.io.resource.ResourceLoader;

public class EdiacaranTestRunner extends Runner{

	private Class<?> testClass;
	
	private EdiacaranBootstrap ediacaranBootstrap;
	
	//private EdiacaranListeners listeners;
	
	private PluginManager pluginManager;
	
    public EdiacaranTestRunner(Class<?> testClass) {
    	super();
        this.testClass = testClass;
    }
    
	@Override
	public Description getDescription() {
		return Description
		          .createTestDescription(testClass, "Ediacaran runner");
	}

	@Override
	public void run(RunNotifier notifier) {
		
		startApplication();
		
		Object testObject = createTestObject();
		
        for (Method method : testClass.getMethods()) {
        	
            if (method.isAnnotationPresent(Test.class)) {
            	executeTest(testObject, method, notifier);
            }
            
        }
        
	}

	private Object createTestObject() {
		
    	Object testObject;
    	
    	try {
    		testObject = testClass.newInstance();
    	}
    	catch(Exception e) {
    		throw new RuntimeException(e);
    	}

    	return testObject;
    	
	}

	private void executeTest(Object testObject, Method method, RunNotifier notifier) {
		
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, testClass, method);
		ClassLoader classLoader = null;
		
		if(contextVars != null) {
			classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
		}
		 
    	notifier.fireTestStarted(Description
    			.createTestDescription(testClass, method.getName()));

    	ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    	
		if(contextVars != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
    	
    	try {
        	method.invoke(testObject);
    	}
    	catch(Exception e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, method.getName()), e));
    		throw new RuntimeException(e);
    	}
    	finally {
    		if(contextVars != null) {
    			Thread.currentThread().setContextClassLoader(oldClassLoader);
    		}
    	}

    	notifier.fireTestFinished(Description
    			.createTestDescription(testClass, method.getName()));
    	
    	
	}
	
	private void startApplication() {
		
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		
		String config = getConfigPath();
		
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
		

		Map<String,Object> params = getParameters();
		
		ediacaranBootstrap.startApplication(params);
		
		//this.listeners = ediacaranBootstrap.getListenerManager();
		this.pluginManager = ediacaranBootstrap.getPluginManager();
	}
	
	private String getConfigPath() {
		ApplicationConfigTest config = testClass.getDeclaredAnnotation(ApplicationConfigTest.class);
		return config == null? ResourceLoader.CLASSPATH_URL_PREFIX + "META-INF/ediacaran-config.xml" : config.value();
	}

	private Map<String,Object> getParameters() {
		
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

	private Map<String,Object> getPluginConfigVars(PluginManager pluginManager, Class<?> clazz, Method m){
		
		PluginContext context = m != null? m.getDeclaredAnnotation(PluginContext.class) : null;
		
		if(context == null && clazz.isAnnotationPresent(PluginContext.class)) {
			context = clazz.getDeclaredAnnotation(PluginContext.class);
		}
	
		return context == null? null : pluginManager.getPluginConfigVars(context.value());
	}
}
