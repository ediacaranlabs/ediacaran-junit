package br.com.uoutec.ediacaran.junit.junit5;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

import br.com.uoutec.ediacaran.junit.EdiacaranInstance;

public class EdiacaranExt 
	implements InvocationInterceptor, BeforeAllCallback, AfterAllCallback, 
	ParameterResolver, TestInstanceFactory {

	private EdiacaranInstance ediacaran;
	
	public EdiacaranExt() throws Throwable {
		ediacaran = new EdiacaranInstance();
	}
	
	 public void interceptTestMethod(Invocation<Void> invocation,
	            ReflectiveInvocationContext<Method> invocationContext,
	            ExtensionContext extensionContext) throws Throwable {
		 invocation.proceed();
    }

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		try {
			ediacaran.destroy();
		}
		catch(Throwable ex) {
			throw new Exception(ex);
		}
	}


	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		try {
			ediacaran.start(context.getTestClass().get());
		}
		catch(Throwable ex) {
			throw new Exception(ex);
		}
	}


	@Override
	public boolean supportsParameter(ParameterContext parameterContext, 
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return true;
	}


	@Override
	public Object resolveParameter(ParameterContext parameterContext, 
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return null;
	}
	
	@Override
	public Object createTestInstance(TestInstanceFactoryContext factoryContext, 
			ExtensionContext extensionContext)
			throws TestInstantiationException {
		return ediacaran.getTestInstance();
	}
	
}
