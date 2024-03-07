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
		Method method  = (Method)params[2];
		Object[] param = (Object[]) params[3];
		ClassLoader classLoader = (ClassLoader)params[4];

		type = classLoader.loadClass(type.getName());
		method = getMethod(method, type, classLoader);
		param = getParameters(method, param, classLoader);
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
	
    private Object[] getParameters(Method m, Object[] params, ClassLoader classLoader) throws Exception {
    	
    	Class<?>[] paramsType = m.getParameterTypes();
    	
    	for(int i=0;i< paramsType.length;i++) {
    		
    		if(params[i] != null) {
    			continue;
    		}
    		
    		Class<?> paramType = paramsType[i];
    		Named named = m.getAnnotatedParameterTypes()[i].getAnnotation(Named.class);
    		
    		params[i] = getParameter(
					named == null? null : named.value(), 
							paramType, 
							classLoader
					);
    		
    	}
    	
    	return params;
    }

	protected Object getParameter(String name, Class<?> type, ClassLoader classLoader) throws Exception {
		return SecurityActionExecutor.run(
				EntityContextPluginAction.class, classLoader, type, name, classLoader);
	}
    
}
