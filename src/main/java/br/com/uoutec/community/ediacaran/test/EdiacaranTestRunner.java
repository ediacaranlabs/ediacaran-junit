package br.com.uoutec.community.ediacaran.test;

import java.beans.XMLDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
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
	
	private Method before;
	
	private Method after;
	
    public EdiacaranTestRunner(Class<?> testClass) {
        init(testClass);
    }
    
    private void init(Class<?> testClass) {
    	
    	this.testClass = testClass;
    	
    	for(Method m: testClass.getDeclaredMethods()) {
    		if(m.isAnnotationPresent(Before.class)) {
    			before = m;
    		}
    		else
    		if(m.isAnnotationPresent(After.class)) {
    			after = m;
    		}
    	}
    }
    
    private void before(Object test, RunNotifier notifier) throws Throwable {
    	if(before == null) {
    		return;
    	}
    	
		try {
			before.invoke(test);
		}
		catch (InvocationTargetException e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, before.getName()), e.getTargetException()));
			throw e;
		}
		catch (Throwable e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, before.getName()), e));
			throw e;
		}
    }

    private void after(Object test, RunNotifier notifier) throws Throwable {
    	if(after == null) {
    		return;
    	}
    	
		try {
			after.invoke(test);
		}
		catch (InvocationTargetException e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, before.getName()), e.getTargetException()));
			throw e;
		}
		catch (Throwable e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, before.getName()), e));
			throw e;
		}
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

            	notifier.fireTestStarted(Description
            			.createTestDescription(testClass, method.getName()));
            	
            	try {
            		runBare(method, testObject, notifier);
            	}
            	catch(Throwable ex) {
                	notifier.fireTestFailure(
                			new Failure(Description
                					.createTestDescription(testClass, method.getName()), ex));
                	continue;
            	}
            	
            	notifier.fireTestFinished(Description
            			.createTestDescription(testClass, method.getName()));

            }
            
        }
        
	}

	private void runBare(Method method, Object testObject, 
			RunNotifier notifier) throws Throwable {
		
    	Throwable exception = null;
    	
    	before(testObject, notifier);
    	
    	try {
    		executeTest(testObject, method, notifier);
    	}
    	catch(Throwable ex) {
    		exception = ex;
    	}
    	finally {
    		try {
    			after(testObject, notifier);
    		}
    		catch(Throwable ex) {
    			if(exception == null) {
    				exception = ex;
    			}
    				
    		}
    	}
    	
    	if (exception != null) { 
    		throw exception;	
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

	private void executeTest(Object testObject, Method method, 
			RunNotifier notifier) throws Throwable {
		
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, testClass, method);
		ClassLoader classLoader = null;
		
		if(contextVars != null) {
			classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
		}
		 
    	ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    	
		if(contextVars != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
    	
    	try {
        	method.invoke(testObject);
    	}
    	finally {
    		if(contextVars != null) {
    			Thread.currentThread().setContextClassLoader(oldClassLoader);
    		}
    	}

    	
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
