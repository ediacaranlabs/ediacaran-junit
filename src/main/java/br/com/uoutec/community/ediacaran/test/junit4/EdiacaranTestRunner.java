package br.com.uoutec.community.ediacaran.test.junit4;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import br.com.uoutec.community.ediacaran.EdiacaranBootstrap;
import br.com.uoutec.community.ediacaran.PluginManager;
import br.com.uoutec.community.ediacaran.plugins.EntityContextPlugin;
import br.com.uoutec.community.ediacaran.plugins.PluginInitializer;
import br.com.uoutec.community.ediacaran.test.ApplicationConfigParameterTest;
import br.com.uoutec.community.ediacaran.test.ApplicationConfigParametersTest;
import br.com.uoutec.community.ediacaran.test.ApplicationConfigTest;
import br.com.uoutec.community.ediacaran.test.PluginContext;
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
	
	private boolean runInContext;
	
    public EdiacaranTestRunner(Class<?> testClass) {
        init(testClass);
    }
    
    private void init(Class<?> testClass) {
    	
    	this.runInContext = false;
    	
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
			executeMethod(test, before);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
		catch (Throwable e) {
			throw e;
		}
    }

    private void after(Object test, RunNotifier notifier) throws Throwable {
    	
    	if(after == null) {
    		return;
    	}
    	
		try {
			executeMethod(test, after);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
		catch (Throwable e) {
			throw e;
		}
    }
    
    private void executeMethod(Object o, Method m
    		) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	
    	Class<?>[] paramsType = m.getParameterTypes();
    	Object[] paramsVals = new Object[paramsType.length];
    	
    	if(paramsType.length > 0 && !runInContext) {
    		throw new RuntimeException("plugin context must be informed!");
    	}
    	
    	for(int i=0;i< paramsType.length;i++) {
    		Object value = null;
    		Class<?> paramType = paramsType[i];
    		Named named = m.getAnnotatedParameterTypes()[i].getAnnotation(Named.class);
    		
			value = named != null? 
					EntityContextPlugin.getEntity(named.value(), paramType) :
					EntityContextPlugin.getEntity(paramType);
			
			paramsVals[i] = value;
    	}
    	
    	m.invoke(o, paramsVals);
    }
    
	@Override
	public Description getDescription() {
		return Description
		          .createTestDescription(testClass, "Ediacaran runner");
	}

	@Override
	public void run(RunNotifier notifier) {
		
		try {
			startApplication();
		}
		catch(Throwable ex) {
			throw new RuntimeException(ex);
		}
		
        for (Method method : testClass.getMethods()) {
        	
            if (method.isAnnotationPresent(Test.class)) {

            	notifier.fireTestStarted(Description
            			.createTestDescription(testClass, method.getName()));
            	
            	try {
            		runInContext(testClass, method, notifier);
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

	private void runInContext(Class<?> testClass, Method method, 
			RunNotifier notifier) throws Throwable {
		
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, testClass, method);
		ClassLoader classLoader = null;
		
		if(contextVars != null) {
			classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
		}
		 
    	ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    	
		if(contextVars != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
    		runInContext = true;
		}
    	
    	try {
    		testClass = getClassContext(testClass, classLoader);
    		method = getMethodContext(testClass, method, classLoader);
    		
    		Object testObject = createTestObject(testClass);
    		runBare(testObject, method, notifier);
    	}
    	finally {
    		if(contextVars != null) {
    			Thread.currentThread().setContextClassLoader(oldClassLoader);
        		runInContext = false;
    		}
    	}
    	
	}
	
	private Method getMethodContext(Class<?> contextClass, Method method, ClassLoader classLoader) throws NoSuchMethodException, SecurityException {
		
		Class<?>[] params = method.getParameterTypes();
		Class<?>[] contextParams = new Class<?>[params.length];
		
		for(int i=0;i<params.length;i++) {
			try {
				contextParams[i] = classLoader.loadClass(params[i].getName());
			}
			catch(Throwable ex) {
				contextParams[i] = params[i];
			}
		}
		
		return contextClass.getMethod(method.getName(), contextParams);
	}
	
	private void runBare(Object testObject, Method method, 
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
	
	private Object createTestObject(Class<?> testClass) {
		
    	Object testObject;
    	
    	try {
    		testObject = testClass.newInstance();
    	}
    	catch(Exception e) {
    		throw new RuntimeException(e);
    	}

    	return testObject;
    	
	}

	private Class<?> getClassContext(Class<?> testClass, ClassLoader classLoader) {
    	try {
    		return classLoader.loadClass(testClass.getName());
    	}
    	catch(Exception e) {
    		throw new RuntimeException(e);
    	}
	}
	
	private void executeTest(Object testObject, Method method, 
			RunNotifier notifier) throws Throwable {
		
		try {
			executeMethod(testObject, method);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
		catch (Throwable e) {
			throw e;
		}
		
	}
	
	private void startApplication() throws Throwable{
		
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
		
		applyDefaultConfiguration(params);
		
		ediacaranBootstrap.loadApplication(params);
		ediacaranBootstrap.startApplication();
		
		this.pluginManager = ediacaranBootstrap.getPluginManager();
	}
	
	private String getConfigPath() {
		ApplicationConfigTest config = testClass.getDeclaredAnnotation(ApplicationConfigTest.class);
		return config == null? "ediacaran/config/ediacaran-config.xml" : config.value();
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
	
	private Map<String,Object> getPluginConfigVars(PluginManager pluginManager, Class<?> clazz, Method m){
		
		PluginContext context = m != null? m.getDeclaredAnnotation(PluginContext.class) : null;
		
		if(context == null && clazz.isAnnotationPresent(PluginContext.class)) {
			context = clazz.getDeclaredAnnotation(PluginContext.class);
		}
	
		return context == null? null : pluginManager.getPluginConfigVars(context.value());
	}
}
