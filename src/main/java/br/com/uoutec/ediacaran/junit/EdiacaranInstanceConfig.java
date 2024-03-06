package br.com.uoutec.ediacaran.junit;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import br.com.uoutec.application.io.Vfs;
import br.com.uoutec.ediacaran.core.EdiacaranBootstrap;

public class EdiacaranInstanceConfig {

	private File[] bases;
	
	public EdiacaranInstanceConfig() {
		this.bases = new File[] {
				new File(System.getProperty("user.dir"),"ediacaran"),
				new File("/develop/ediacaran"),
				new File("/ediacaran"),
		};
	}
	
	public Map<String,Object> getParameters(Class<?> testClass) throws MalformedURLException {
		
		Map<String,Object> r = new HashMap<String,Object>();
		
		ApplicationConfigParametersTest params = 
				testClass.getDeclaredAnnotation(ApplicationConfigParametersTest.class);
		
		if(params != null) {
			for(ApplicationConfigParameterTest p: params.value()) {
				r.put(p.paramName(), p.paramValue());
			}
		}
		
		ApplicationConfigParameterTest param = 
				testClass.getDeclaredAnnotation(ApplicationConfigParameterTest.class);

		if(param != null) {
			r.put(param.paramName(), param.paramValue());
		}
		
		applyDefaultConfiguration(r);
		
		return r;
	}

	private void applyDefaultConfiguration(Map<String, Object> contextParams) throws MalformedURLException {
		
		if(!contextParams.containsKey("app")) {
				contextParams.put(
					"app",
					getPath("config" + Vfs.getSeparator() +	"ediacaran-config.xml").toURI().toURL().toExternalForm()
				);
		}
		
		if(!contextParams.containsKey(EdiacaranBootstrap.CONFIG_FILE_VAR)) {
			contextParams.put(
				EdiacaranBootstrap.CONFIG_FILE_VAR, 
				getPath("config" + Vfs.getSeparator() +	"ediacaran-dev.properties").toURI().toURL().toExternalForm()
			);
		}
		
		if(!contextParams.containsKey(EdiacaranBootstrap.LOGGER_CONFIG_FILE_VAR)) {
			contextParams.put(
				EdiacaranBootstrap.LOGGER_CONFIG_FILE_VAR,
				getPath("config" + Vfs.getSeparator() +	"log4j.configuration").toURI().toURL().toExternalForm()
			);
		}

		if(!contextParams.containsKey(EdiacaranBootstrap.CONFIG_PATH_PROPERTY)) {
			contextParams.put(
				EdiacaranBootstrap.CONFIG_PATH_PROPERTY,
				getPath("config").toURI().toURL().toExternalForm()
			);
		}
		
		if(!contextParams.containsKey(EdiacaranBootstrap.BASE_PATH_PROPERTY)) {
			contextParams.put(
				EdiacaranBootstrap.BASE_PATH_PROPERTY, 
				getPath(null).toURI().toURL().toExternalForm()
				);
		}
		
		if(System.getProperty("system.security.policy") == null) {
			System.setProperty(
					"system.security.policy", 
					getPath("config" + Vfs.getSeparator() +	"security.json").toURI().toURL().getPath());
		}
		
		if(System.getProperty("system.security.print") == null) {
			System.setProperty(
					"system.security.print", 
					"true");
		}

		if(System.getProperty("system.security") == null) {
			System.setProperty(
					"system.security", 
					"true");
		}
		
		if(System.getProperty("system.security.debug") == null) {
			System.setProperty(
					"system.security.debug", 
					"error");
		}
		
	}
	
	private File getPath(String path) {
		
		for(File base: bases) {
			File p = path == null? base : new File(base, path);
			p = p.getAbsoluteFile();
			if(p.exists()) {
				return p;
			}
		}
		
		throw new RuntimeException(path);
	}
	
}
