package br.com.uoutec.ediacaran.junit.junit4;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import br.com.uoutec.ediacaran.junit.EdiacaranInstance;

public class EdiacaranTestRunner extends Runner {

	private Class<?> testClass;

	private EdiacaranInstance ediacaran;
	
	private Method beforeAll;
	
	private Method before;
	
	private Method afterAll;
	
	private Method after;
	
    public EdiacaranTestRunner(Class<?> testClass) {
    	super();
    	init(testClass);
    }
    
    private void init(Class<?> testClass) {
    	
    	this.testClass     = testClass;
		this.ediacaran     = new EdiacaranInstance();
    	
    	for(Method m: testClass.getDeclaredMethods()) {
    		if(m.isAnnotationPresent(BeforeClass.class)) {
    			beforeAll = m;
    		}
    		else
    		if(m.isAnnotationPresent(AfterClass.class)) {
    			afterAll = m;
    		}
    		else
    		if(m.isAnnotationPresent(Before.class)) {
    			before = m;
    		}
    		else
    		if(m.isAnnotationPresent(After.class)) {
    			after = m;
    		}
    	}
    	
    }
    

	@Override
	public Description getDescription() {
		return Description
		          .createTestDescription(testClass, "Ediacaran runner");
	}

	protected void init(RunNotifier notifier) {
		try {
			ediacaran.start(testClass);
		}
		catch(Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

    protected void beforeAll(Object test, RunNotifier notifier){
    	
    	if(beforeAll == null) {
    		return;
    	}
    	
		try {
			run(beforeAll, test, notifier);
		}
		catch (Throwable e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, beforeAll.getName()), e));
        	throw new RuntimeException("beforeClass", e);
		}
		
    }
	
    protected void afterAll(Object test, RunNotifier notifier) {
    	
    	if(afterAll == null) {
    		return;
    	}
    	
		try {
			run(afterAll, test, notifier);
		}
		catch (Throwable e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, afterAll.getName()), e));
        	throw new RuntimeException("afterAll", e);
		}
		
    }
	
	
    protected void before(Object test, RunNotifier notifier) {
    	
    	if(before == null) {
    		return;
    	}
    	
		try {
			run(before, test, notifier);
		}
		catch (Throwable e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, before.getName()), e));
        	throw new RuntimeException("before", e);
		}
		
    }
	
    protected void after(Object test, RunNotifier notifier) {
    	
    	if(after == null) {
    		return;
    	}
    	
		try {
			run(after, test, notifier);
		}
		catch (Throwable e) {
        	notifier.fireTestFailure(
        			new Failure(Description
        					.createTestDescription(testClass, after.getName()), e));
        	throw new RuntimeException("after", e);
		}
		
    }
    
	protected void destroy(RunNotifier notifier) {
		try {
			ediacaran.destroy();
		}
		catch(Throwable ex) {
			throw new RuntimeException(ex);
		}
	}
	
	protected Object createTestInstance(RunNotifier notifier) throws Throwable {
		return ediacaran.getTestInstance();
	}
	
	@Override
	public void run(RunNotifier notifier) {
		init(notifier);
		try {
			Object test = createTestInstance(notifier);
			beforeAll(test, notifier);
			run(test, notifier);
			afterAll(test, notifier);
		}
		catch(Throwable ex) {
			throw new RuntimeException(ex);
		}
		finally {
			destroy(notifier);
		}
	}

	protected void run(Object test, RunNotifier notifier) {
		
        for (Method method : testClass.getMethods()) {
        	notifier.addListener(null);
            if (method.isAnnotationPresent(Test.class)) {

        		before(test, notifier);
        		
            	try {
                	notifier.fireTestStarted(Description
                			.createTestDescription(testClass, method.getName()));
                	
            		run(method, test, notifier);
            		
                	notifier.fireTestFinished(Description
                			.createTestDescription(testClass, method.getName()));
            	}
            	catch(Throwable ex) {
                	notifier.fireTestFailure(
                			new Failure(Description
                					.createTestDescription(testClass, method.getName()), ex));
            	}

        		after(test, notifier);
        		
            }
            
        }
		
	}

	protected void run(Method method, Object test, RunNotifier notifier) throws Throwable {
		method.invoke(test, new Object[method.getParameterCount()]);
		//ediacaran.executeTest(method, test, new Object[method.getParameterCount()]);
	}
	
}
