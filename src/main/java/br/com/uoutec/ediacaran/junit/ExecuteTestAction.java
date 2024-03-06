package br.com.uoutec.ediacaran.junit;

import java.lang.reflect.Method;

import javax.inject.Named;

import br.com.uoutec.application.security.SecurityAction;
import br.com.uoutec.application.security.SecurityActionExecutor;

public class ExecuteTestAction implements SecurityAction {

	@Override
	public Object run(Object... params) throws Exception {
		Object target  = params[0];
		Class<?> type  = (Class<?>)params[1];
		Method method  = (Method) params[2];
		ClassLoader classLoader = (ClassLoader)params[3];

		type = classLoader.loadClass(type.getName());
		method = getMethod(method, type, classLoader);
		Object[] param = getParameters(method, classLoader);
		return method.invoke(target, param);
	}
    
	public Method getMethod(Method method, Class<?> type, ClassLoader classLoader
			) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		
		Class<?>[] params = method.getParameterTypes();
		
		for(int i=0;i<params.length;i++) {
			Class<?> p = params[i];
			params[i] = classLoader.loadClass(p.getName());
		}
		
		return type.getMethod(method.getName(), params);
	}
	
    private Object[] getParameters(Method m, ClassLoader classLoader) throws Exception {
    	
    	Class<?>[] paramsType = m.getParameterTypes();
    	Object[] paramsVals = new Object[paramsType.length];
    	
    	for(int i=0;i< paramsType.length;i++) {
    		Class<?> paramType = paramsType[i];
    		Named named = m.getAnnotatedParameterTypes()[i].getAnnotation(Named.class);
    		
			paramsVals[i] = getParameter(
					named == null? null : named.value(), 
							paramType, 
							classLoader
					);
    	}
    	
    	return paramsVals;
    }

	protected Object getParameter(String name, Class<?> type, ClassLoader classLoader) throws Exception {
		return SecurityActionExecutor.run(
				EntityContextPluginAction.class, classLoader, type, name, classLoader);
	}
    
}
