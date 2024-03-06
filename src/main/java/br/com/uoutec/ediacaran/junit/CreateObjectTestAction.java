package br.com.uoutec.ediacaran.junit;

import br.com.uoutec.application.security.SecurityAction;

public class CreateObjectTestAction implements SecurityAction {

	@Override
	public Object run(Object... params) throws Exception {
		Class<?> clazz = (Class<?>) params[0];
		return clazz.getConstructor().newInstance();
	}

}
