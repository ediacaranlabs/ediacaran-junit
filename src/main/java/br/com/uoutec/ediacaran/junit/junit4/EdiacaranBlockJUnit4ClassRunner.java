package br.com.uoutec.ediacaran.junit.junit4;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import br.com.uoutec.ediacaran.junit.EdiacaranInstance;

public class EdiacaranBlockJUnit4ClassRunner 
	extends BlockJUnit4ClassRunner {

	private EdiacaranInstance ediacaran;

	public EdiacaranBlockJUnit4ClassRunner(Class<?> testClass
			) throws InitializationError {
		super(testClass);
		this.ediacaran = new EdiacaranInstance();
	}

    protected Object createTest() throws Exception {
        return ediacaran.getTestInstance();
    }
    
    /*
	@Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return super.methodInvoker(method, test);
    }
	*/
    
    public void run(final RunNotifier notifier) {
    	try {
    		ediacaran.start(getTestClass().getJavaClass());
    		super.run(notifier);
    	}
    	catch(Throwable ex) {
    		throw new RuntimeException(ex);
    	}
    	finally {
    		ediacaran.destroy();
    	}
    }
	
}
