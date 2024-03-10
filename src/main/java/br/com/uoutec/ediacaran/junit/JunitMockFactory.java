package br.com.uoutec.ediacaran.junit;

import java.util.ArrayList;
import java.util.List;

import br.com.uoutec.application.bean.Bean;
import br.com.uoutec.application.bean.BeanPropertyAnnotation;
import br.com.uoutec.community.ediacaran.test.mock.Mock;

public class JunitMockFactory {

	private Bean bean;
	
	public JunitMockFactory(Object mockProvider) {
		this.bean = new Bean(mockProvider);
	}
	
	public Object[] getMocksByName(String name) throws Exception {
		return getMocks(null, name);
	}
	
	public Object[] getMocksByType(Class<?> type) throws Exception {
		return getMocks(type, null);
	}
	
	public Object[] getMocks(Class<?> type, String name) throws Exception {
		List<Object> result = new ArrayList<>();
		List<BeanPropertyAnnotation> props = bean.getProperties();
		for(BeanPropertyAnnotation bpa: props) {
			Mock mockAnnotation = bpa.getAnnotation(Mock.class);
			
			if(mockAnnotation == null) {
				continue;
			}
			
			if(
				(name == null || name.equals(mockAnnotation.value())) && 
				(type == null || bpa.getType().equals(type))
			) {
				result.add(bean.get(bpa.getName()));
			}
			
		}
		
		return result.stream().toArray(Object[]::new);
	}
}
