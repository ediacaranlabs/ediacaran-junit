package br.com.uoutec.ediacaran.junit;

import br.com.uoutec.application.security.SecurityAction;
import br.com.uoutec.ediacaran.core.plugins.EntityContextPlugin;

public class EntityContextPluginAction 
	implements SecurityAction {

	@Override
	public Object run(Object... params) throws Exception {
		
		Class<?> clazz = (Class<?>) params[0];
		String name = (String)params[1];
		ClassLoader classLoader = (ClassLoader)params[2];
		clazz = classLoader.loadClass(clazz.getName());
		
		return name != null?
				EntityContextPlugin.getEntity(name, clazz) :
				EntityContextPlugin.getEntity(clazz);
	}

}
