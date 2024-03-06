package br.com.uoutec.ediacaran.junit;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import br.com.uoutec.application.se.ApplicationBootstrapProxy;
import br.com.uoutec.application.security.SecurityActionExecutor;
import br.com.uoutec.application.security.SecurityThreadExecutor;
import br.com.uoutec.community.ediacaran.test.mock.MockBeanDiscover;
import br.com.uoutec.community.ediacaran.test.mock.SecurityPolicyManagerMock;
import br.com.uoutec.ediacaran.core.EdiacaranBootstrap;
import br.com.uoutec.ediacaran.core.PluginManager;
import br.com.uoutec.ediacaran.core.plugins.PluginInitializer;

public class EdiacaranInstance {

	private EdiacaranBootstrap ediacaranBootstrap;
	
	private PluginManager pluginManager;
	
	private Class<?> testClass;
	
	private Map<String,Object> params;
	
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
			ediacaranBootstrap.loadApplication(params);
			ediacaranBootstrap.startApplication();
			this.pluginManager = (PluginManager)ediacaranBootstrap.getPluginManager();
		});
	}
	
	private void loadConfiguration() {
		try {
			EdiacaranInstanceConfig eic = new EdiacaranInstanceConfig();
			this.params = eic.getParameters(testClass);
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
			
			ediacaranBootstrap.setSecurityPolicyManager(new SecurityPolicyManagerMock(pluginContext, testClass));
		});		
	}
	
	private void createApplication() {
		executeInSecurityThread(()->{
			Map<String,Object> map = new HashMap<String,Object>(this.params);
			ApplicationBootstrapProxy app = new ApplicationBootstrapProxy("app");
			app.loadApplication(map);
			this.ediacaranBootstrap = (EdiacaranBootstrap)app.getApplicationBootstrap();
		});		
	}
	
	private String getPluginContext(Class<?> testClass) {
		
		PluginContext pc = testClass.getAnnotation(PluginContext.class);
		if(pc != null && !pc.value().isEmpty()) {
			return pc.value();
		}
		
		return null;
	}

	public Object getTestInstance(String context) throws Throwable {
		
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, context);
		ClassLoader classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
		
		return execute(()->{
			
			return SecurityActionExecutor.run(
					EntityContextPluginAction.class, classLoader, testClass, null, classLoader);
			
		}, context);
	}

	public void executeTest(Method method, Object test,	String context) throws Throwable {
		
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, context);
		ClassLoader classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);

		execute(()->{
			
			return SecurityActionExecutor.run(
					ExecuteTestAction.class, classLoader, test, testClass, method, classLoader);
			
		}, context);
		
	}
	
	public Method getMethod(Method method, Class<?> testClass, String context) throws Throwable {
		
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, context);
		ClassLoader classLoader = null;
		
		if(contextVars != null) {
			classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
		}

		Class<?> type =	classLoader.loadClass(testClass.getName());
		
		Class<?>[] params = method.getParameterTypes();
		
		for(int i=0;i<params.length;i++) {
			Class<?> p = params[i];
			params[i] = classLoader.loadClass(p.getName());
		}
		
		return type.getMethod(method.getName(), params);
	}
	
	public Object execute(Callable<Object> value, String context) throws Throwable {
	
		Map<String,Object> contextVars = getPluginConfigVars(this.pluginManager, context);
		ClassLoader classLoader = null;
		
		if(contextVars != null) {
			classLoader = (ClassLoader)contextVars.get(PluginInitializer.CLASS_LOADER);
		}
		 
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

	private void executeInSecurityThread(Runnable value) {
        SecurityThreadExecutor ste = new SecurityThreadExecutor(value, true);
        ste.start();
	}
	
	private void executeInSecurityThread(Runnable value, ClassLoader classLoader) {
        SecurityThreadExecutor ste = new SecurityThreadExecutor(value, classLoader, true);
        ste.start();
	}
	
}
