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

import br.com.uoutec.application.javassist.JavassistCodeGenerator;
import br.com.uoutec.application.proxy.CodeGenerator;
import br.com.uoutec.application.proxy.ProxyFactory;
import br.com.uoutec.application.proxy.SecurityProxyHandler;
import br.com.uoutec.application.security.SecurityClassLoader;
import br.com.uoutec.application.security.SystemSecurityClassLoaderProvider;
import br.com.uoutec.ediacaran.junit.EdiacaranInstance;
import br.com.uoutec.ediacaran.junit.JunitProxyHandler;
import br.com.uoutec.ediacaran.junit.PluginContext;

public class EdiacaranExt 
	implements InvocationInterceptor, BeforeAllCallback, AfterAllCallback, 
	ParameterResolver, TestInstanceFactory {

	private CodeGenerator codeGenerator;
	
	private EdiacaranInstance ediacaran;
	
	private ClassLoader classLoader;
	
	public EdiacaranExt() throws Throwable {
		
		this.classLoader = getClass().getClassLoader();//SystemSecurityClassLoader.getDefaultSystemSecurityClassloader();
		
		ediacaran = (EdiacaranInstance) SecurityClassLoader
				.getDefaultcodegenerator()
				.getProxyFactory(EdiacaranInstance.class)
				.getNewProxy(
						new SecurityProxyHandler(
								SecurityClassLoader.getDefaultcodegenerator(), 
								getClass().getClassLoader(), 
								SystemSecurityClassLoaderProvider.getDefaultSystemSecurityClassloader().getCodeGenerator(), 
								this.classLoader,
								new EdiacaranInstance()
								//this.classLoader
								//	.loadClass(EdiacaranInstance.class.getName())
								//		.getConstructor().newInstance()
						)
				);
		
		codeGenerator = new JavassistCodeGenerator();
	}
	
	 public void interceptTestMethod(Invocation<Void> invocation,
	            ReflectiveInvocationContext<Method> invocationContext,
	            ExtensionContext extensionContext) throws Throwable {

		String context = 
				getContextName(
						extensionContext.getTestClass().get(), 
						extensionContext.getTestMethod().get().getAnnotation(PluginContext.class)
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
	public void afterAll(ExtensionContext context) throws Exception {
		try {
			ediacaran.destroy();
		}
		catch(Throwable ex) {
		}
	}


	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		try {
			Class<?> testClass = 
					this.classLoader
					.loadClass(context.getTestClass().get().getName());
			//ediacaran.start(context.getTestClass().get());
			ediacaran.start(testClass);
		}
		catch(Throwable ex) {
			throw new Exception(ex);
		}
	}


	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return true;
	}


	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return null;
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
		
		ProxyFactory proxyFactory = codeGenerator.getProxyFactory(type);
		
		return proxyFactory.getNewProxy(new JunitProxyHandler(ediacaran));
	}
	
}
