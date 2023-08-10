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

import br.com.uoutec.application.javassist.JavassistCodeGenerator;
import br.com.uoutec.application.proxy.CodeGenerator;
import br.com.uoutec.application.proxy.ProxyFactory;
import br.com.uoutec.community.ediacaran.EdiacaranBootstrap;
import br.com.uoutec.community.ediacaran.PluginManager;
import br.com.uoutec.community.ediacaran.plugins.EntityContextPlugin;
import br.com.uoutec.community.ediacaran.plugins.PluginInitializer;
import br.com.uoutec.community.ediacaran.test.ApplicationConfigParameterTest;
import br.com.uoutec.community.ediacaran.test.ApplicationConfigParametersTest;
import br.com.uoutec.community.ediacaran.test.ApplicationConfigTest;
import br.com.uoutec.community.ediacaran.test.EdiacaranInstance;
import br.com.uoutec.community.ediacaran.test.JunitProxyHandler;
import br.com.uoutec.community.ediacaran.test.PluginContext;
import br.com.uoutec.io.resource.DefaultResourceLoader;
import br.com.uoutec.io.resource.Resource;
import br.com.uoutec.io.resource.ResourceLoader;

public class EdiacaranTestRunner extends Runner{

	private Class<?> testClass;

	private CodeGenerator codeGenerator;
	
	private EdiacaranInstance ediacaran;
	
	private Method before;
	
	private Method after;
	
	private boolean runInContext;
	
    public EdiacaranTestRunner(Class<?> testClass) {
        init(testClass);
    }
    
    private void init(Class<?> testClass) {
    	
    	this.runInContext = false;
    	
    	this.testClass     = testClass;
		this.ediacaran     = new EdiacaranInstance();
		this.codeGenerator = new JavassistCodeGenerator();
    	
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
			ediacaran.start(testClass);
		}
		catch(Throwable ex) {
			throw new RuntimeException(ex);
		}
		
		Object testObject = createTestObject(testClass);
		
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

	private void runInContext(Object testObject, Method method, 
			RunNotifier notifier) throws Throwable {
		
    	
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
		ProxyFactory proxyFactory = codeGenerator.getProxyFactory(testClass);
		return proxyFactory.getNewProxy(new JunitProxyHandler(ediacaran));
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

}
