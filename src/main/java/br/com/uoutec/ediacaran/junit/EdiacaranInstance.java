package br.com.uoutec.ediacaran.junit;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import br.com.uoutec.application.ApplicationThread;
import br.com.uoutec.application.javassist.JavassistCodeGenerator;
import br.com.uoutec.application.proxy.CodeGenerator;
import br.com.uoutec.application.proxy.ProxyFactory;
import br.com.uoutec.application.security.ContextSystemSecurityCheck;
import br.com.uoutec.application.security.SecurityActionExecutor;
import br.com.uoutec.community.ediacaran.test.mock.EdiacaranBootstrapDiscover;
import br.com.uoutec.community.ediacaran.test.mock.MockBeanDiscover;
import br.com.uoutec.ediacaran.core.EdiacaranBootstrap;
import br.com.uoutec.ediacaran.core.PluginManager;
import br.com.uoutec.ediacaran.core.plugins.PluginInitializer;

public class EdiacaranInstance {

	private CodeGenerator codeGenerator;

	private EdiacaranBootstrap ediacaranBootstrap;
	
	private PluginManager pluginManager;
	
	private Class<?> testClass;
	
	private Map<String,Object> params;
	
	public EdiacaranInstance() {
		this.codeGenerator = new JavassistCodeGenerator();
	}
	
	public void destroy() {
		if(ediacaranBootstrap != null) {
			executeInSecurityThread(()->{
				ediacaranBootstrap.stopApplication();
			});
		}
	}
	
	public void start(Class<?> testClass) throws Throwable{
		this.testClass = testClass;
		loadConfiguration();
		createApplication();
		registerMocks();
		startApplication();
	}
	
	private void startApplication() {
		executeInSecurityThread(()->{
			this.ediacaranBootstrap.startApplication();
			this.pluginManager = (PluginManager)ediacaranBootstrap.getPluginManager();
		});
	}
	
	private void loadConfiguration() {
		try {
			ApplicationStartConfigJunit eic = new ApplicationStartConfigJunit();
			this.params = new HashMap<String,Object>();
			eic.applyDefaultConfiguration(testClass, this.params);
		}
		catch(Throwable ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private void registerMocks() {
		executeInSecurityThread(()->{
			String pluginContext = getPluginContext(testClass);
			
			MockBeanDiscover mbd = new MockBeanDiscover();
			
			Map<Class<?>, Object> mocks =  mbd.getMocks(testClass, pluginContext);
			
			for(Entry<Class<?>, Object> e: mocks.entrySet()) {
				ediacaranBootstrap.addEntity(e.getValue(), e.getKey());	
			}
			
		});		
	}
	
	private void createApplication() {
		executeInSecurityThread(()->{
			Map<String,Object> map = new HashMap<String,Object>(this.params);
			EdiacaranBootstrapDiscover ebd = new EdiacaranBootstrapDiscover();
			this.ediacaranBootstrap =  ebd.getEdiacaranBootstrap(testClass);
			this.ediacaranBootstrap.loadApplication(map);
			this.ediacaranBootstrap.createApplication();
		});		
	}
	
	public Object getTestInstance() {
		try {
			String context = getPluginContext(testClass);
			return getTestInstance(context);
		}
		catch(Throwable ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public Object getTestInstance(String context) throws Throwable {
		
		ClassLoader classLoader = getContextClassLoader(context);
		
		Object instance = execute(()->{
			return SecurityActionExecutor.run(
					EntityContextPluginAction.class, classLoader, testClass, null, classLoader);
		}, context);

		if(instance == null) {
			throw new RuntimeException("test bean not found!");
		}
		
		ProxyFactory proxyFactory = codeGenerator.getProxyFactory(testClass);
		return proxyFactory.getNewProxy(new JunitProxyHandler(this, instance));
	}

	public void executeTest(Method method, Object test, Object[] params) throws Throwable {
		String context = getPluginContext(method);
		executeTest(method, test, params, context);
	}
	
	public void executeTest(Method method, Object test,	Object params, String context) throws Throwable {
		
		ClassLoader classLoader = getContextClassLoader(context);

		execute(()->{
			
			return SecurityActionExecutor.run(
					ExecuteTestAction.class, classLoader, test, testClass, method, params, classLoader);
			
		}, context);
		
	}
	
	public Object execute(Callable<Object> value, String context) throws Throwable {
	
		ClassLoader classLoader = getContextClassLoader(context);
		 
		Throwable[] ex = new Throwable[1];
		Object[] result = new Object[1];
		
		executeInSecurityThread(()->{
			try {
				result[0] = value.call();
			}
			catch(Throwable e) {
				ex[0] = e;
			}
		}, classLoader);
		
		if(ex[0] != null) {
			throw ex[0];
		}
		
		return result[0];
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Object> getPluginConfigVars(PluginManager pluginManager, String context){
		Object[] o = new Object[1];
		executeInSecurityThread(()->{
			o[0] = context == null? null : pluginManager.getPluginConfigVars(context);
		});
		return (Map<String, Object>) o[0];
	}

	public ClassLoader getContextClassLoader(String context) {
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, context);
		return (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
	}
	
	private void executeInSecurityThread(Runnable value) {
		ApplicationThread app = 
				ContextSystemSecurityCheck.doPrivileged(()->new ApplicationThread(value)); 
		
		app.start(true);
		
        if(app.getException() != null) {
        	throw new RuntimeException(app.getException());
        }
	}
	
	private void executeInSecurityThread(Runnable value, ClassLoader classLoader) {
		ApplicationThread app = 
				ContextSystemSecurityCheck.doPrivileged(()->new ApplicationThread(value, classLoader)); 
		
		app.start(true);
		
        if(app.getException() != null) {
        	throw new RuntimeException(app.getException());
        }
	}
	
	private String getPluginContext(Class<?> testClass) {
		
		PluginContext pc = testClass.getAnnotation(PluginContext.class);
		if(pc != null && !pc.value().isEmpty()) {
			return pc.value();
		}
		
		return null;
	}

	private String getPluginContext(Method method) {
		
		PluginContext pc = method.getAnnotation(PluginContext.class);
		
		if(pc == null || pc.value().isEmpty()) {
			pc = method.getDeclaringClass().getAnnotation(PluginContext.class);
		}
		
		return pc == null? null : pc.value();
	}
	
}
