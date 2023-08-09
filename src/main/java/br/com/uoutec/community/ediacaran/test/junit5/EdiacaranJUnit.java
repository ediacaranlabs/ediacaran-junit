package br.com.uoutec.community.ediacaran.test.junit5;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstantiationException;

import br.com.uoutec.community.ediacaran.plugins.EntityContextPlugin;
import br.com.uoutec.community.ediacaran.test.EdiacaranInstance;
import br.com.uoutec.community.ediacaran.test.PluginContext;

public class EdiacaranJUnit 
	implements TestInstancePostProcessor, 
	InvocationInterceptor, BeforeAllCallback, AfterAllCallback, 
	BeforeEachCallback, AfterEachCallback, ParameterResolver, TestInstanceFactory {

	private EdiacaranInstance ediacaran;
	
	public EdiacaranJUnit() {
		ediacaran = new EdiacaranInstance();
	}
	
	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
	}


	 public void interceptTestMethod(Invocation<Void> invocation,
	            ReflectiveInvocationContext<Method> invocationContext,
	            ExtensionContext extensionContext) throws Throwable {

		String context = 
				getContextName(
						invocationContext.getTargetClass(), 
						invocationContext.getExecutable().getAnnotation(PluginContext.class)
				);

		ediacaran.execute(()->{
			try {
				return invocation.proceed();
			}
			catch(Throwable ex) {
				throw new Exception(ex);
			}
		}, context);
    }


	@Override
	public void afterEach(ExtensionContext context) throws Exception {
	}


	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
	}


	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		ediacaran.destroy();
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
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		//return parameterContext.getParameter().getType().equals(EmployeeJdbcDao.class);
		return getContextName(
				parameterContext.getDeclaringExecutable().getDeclaringClass(), 
				parameterContext.getDeclaringExecutable().getAnnotation(PluginContext.class)
		) != null;
	}


	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		
		Class<?> type = parameterContext.getParameter().getType();
		
		String context = 
				getContextName(
						parameterContext.getDeclaringExecutable().getDeclaringClass(), 
						parameterContext.getDeclaringExecutable().getAnnotation(PluginContext.class)
				);
		
		try {
			Object param = ediacaran.execute(()->{
				Class<?> contextType = Thread.currentThread().getContextClassLoader().loadClass(type.getName());
				return EntityContextPlugin.getEntity(contextType);
			}, context);
			
			return param;
		}
		catch(Throwable ex) {
			throw new ParameterResolutionException("parameter error", ex);
		}
		
	}
	
	private String getContextName(Class<?> clazz, PluginContext context){
		
		if(context == null && clazz.isAnnotationPresent(PluginContext.class)) {
			context = clazz.getDeclaredAnnotation(PluginContext.class);
		}
	
		return context == null? null : context.value();
	}

	@Override
	public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
			throws TestInstantiationException {
		
		Class<?> type = factoryContext.getTestClass();
		
		String context = 
				getContextName(
						factoryContext.getTestClass(), 
						null
				);
		
		try {
			Object param = ediacaran.execute(()->{
				Class<?> contextType = Thread.currentThread().getContextClassLoader().loadClass(type.getName());
				return EntityContextPlugin.getEntity(contextType);
			}, context);
			
			return param;
		}
		catch(Throwable ex) {
			throw new ParameterResolutionException("parameter error", ex);
		}
	}
	
}
