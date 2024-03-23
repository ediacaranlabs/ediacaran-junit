package br.com.uoutec.ediacaran.junit;

import java.net.MalformedURLException;
import java.util.Map;

import br.com.uoutec.ediacaran.core.ApplicationStartConfig;

public class ApplicationStartConfigJunit extends ApplicationStartConfig {

	public void applyDefaultConfiguration(Class<?> testClass, 
			Map<String,Object> contextParams) throws MalformedURLException {
		
		ApplicationConfigParametersTest params = 
				testClass.getDeclaredAnnotation(ApplicationConfigParametersTest.class);
		
		if(params != null) {
			for(ApplicationConfigParameterTest p: params.value()) {
				contextParams.put(p.paramName(), p.paramValue());
			}
		}
		
		ApplicationConfigParameterTest param = 
				testClass.getDeclaredAnnotation(ApplicationConfigParameterTest.class);

		if(param != null) {
			contextParams.put(param.paramName(), param.paramValue());
		}
		
		super.applyDefaultConfiguration(contextParams);
	}
	
}
