package br.com.uoutec.ediacaran.junit;

import java.lang.reflect.Method;

import br.com.uoutec.application.proxy.ProxyHandler;

public class JunitProxyHandler implements ProxyHandler{

	private Object target;
	
	private EdiacaranInstance ediacaranInstance;
	
	public JunitProxyHandler(EdiacaranInstance ediacaranInstance, Object target) {
		this.ediacaranInstance = ediacaranInstance;
		this.target = target;
	}

	@Override
	public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
		ediacaranInstance.executeTest(thisMethod, target, args);
		return null;
	}

	@Override
	public Object getTarget() {
		return null;
	}

}
