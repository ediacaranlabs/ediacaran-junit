package br.com.uoutec.community.ediacaran.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import br.com.uoutec.application.proxy.ProxyHandler;
import br.com.uoutec.community.ediacaran.plugins.EntityContextPlugin;

public class JunitProxyHandler implements ProxyHandler{

	private final static Set<Class<?>> primitiveType;

	static {
		primitiveType = new HashSet<Class<?>>();
		primitiveType.add(boolean.class);
		primitiveType.add(byte.class);
		primitiveType.add(char.class);
		primitiveType.add(double.class);
		primitiveType.add(float.class);
		primitiveType.add(int.class);
		primitiveType.add(long.class);
		primitiveType.add(short.class);
		primitiveType.add(void.class);

		primitiveType.add(Boolean.class);
		primitiveType.add(Byte.class);
		primitiveType.add(Character.class);
		primitiveType.add(Double.class);
		primitiveType.add(Float.class);
		primitiveType.add(Integer.class);
		primitiveType.add(Long.class);
		primitiveType.add(Short.class);
		primitiveType.add(Void.class);
		primitiveType.add(BigDecimal.class);
		primitiveType.add(BigInteger.class);
		primitiveType.add(String.class);
		primitiveType.add(Class.class);
	
	}
	
	private EdiacaranInstance ediacaranInstance;
	
	public JunitProxyHandler(EdiacaranInstance ediacaranInstance) {
		this.ediacaranInstance = ediacaranInstance;
	}

	@Override
	public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
		
		String context = 
				getContextName(
						thisMethod.getDeclaringClass(), 
						thisMethod.getAnnotation(PluginContext.class)
				);
		
		ediacaranInstance.execute(()->{
			runInContext(thisMethod.getDeclaringClass(), thisMethod);
			return null;
		}, context);
		
		return null;
	}

	@Override
	public Object getTarget() {
		return null;
	}

	private String getContextName(Class<?> clazz, PluginContext context){
		
		if(context == null && clazz.isAnnotationPresent(PluginContext.class)) {
			context = clazz.getDeclaredAnnotation(PluginContext.class);
		}
	
		return context == null? null : context.value();
	}
	
	private void runInContext(Class<?> testClass, Method method) throws Exception {
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		testClass = getClassContext(testClass, classLoader);
		method = getMethodContext(testClass, method, classLoader);
		
		Object testObject = createTestObject(testClass);
		executeMethod(testObject, method);
		
	}
	
	private Class<?> getClassContext(Class<?> testClass, ClassLoader classLoader) {
    	try {
    		return classLoader.loadClass(testClass.getName());
    	}
    	catch(Exception e) {
    		throw new RuntimeException(e);
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

	private Object createTestObject(Class<?> testClass) {
    	return EntityContextPlugin.getEntity(testClass);
    	
	}
	
    private void executeMethod(Object o, Method m
    		) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	
    	Class<?>[] paramsType = m.getParameterTypes();
    	Object[] paramsVals = new Object[paramsType.length];
    	
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
	
}