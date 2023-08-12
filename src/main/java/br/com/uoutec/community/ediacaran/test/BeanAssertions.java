package br.com.uoutec.community.ediacaran.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import br.com.uoutec.application.bean.Bean;
import br.com.uoutec.application.bean.BeanPropertyAnnotation;

public class BeanAssertions {

	public static void assertBeanEquals(Object expected, Object actual) {
		try {
			assertEquals(expected, actual, null);
		}
		catch(AssertionFailedError e) {
			throw e;
		}
		catch(Throwable e) {
			throw new AssertionFailedError("unexpected error", e);
		}
	}
	
	private static void assertEquals(Object expected, Object actual, String path) throws Throwable {
		
		if(expected == null) {
			if(actual != null) {
				throw new AssertionFailedError(path + " => null != " + actual);
			}
		}
		else
		if(actual == null) {
			throw new AssertionFailedError(path + " => " + actual + " != null");
		}
		else
		if(expected.getClass().isAssignableFrom(Map.class)) {
			
			if(!actual.getClass().isAssignableFrom(Map.class)) {
				throw new AssertionFailedError(path  + " => " + expected.getClass() + " " + actual.getClass());
			}
			
			assertEquals((Map<?,?>)expected, (Map<?,?>)actual, path);
			
		}
		else
		if(expected.getClass().isAssignableFrom(Collection.class)) {
			
			if(!actual.getClass().isAssignableFrom(Collection.class)) {
				throw new AssertionFailedError(path + " => " + expected.getClass() + " " + actual.getClass());
			}
			
			assertEquals((Collection<?>)expected, (Collection<?>)actual, path);
		}		
		else
		if(Bean.isPrimitive(expected.getClass())) {
			Assertions.assertEquals(expected, actual);
		}
		else {
			assertEquals(new Bean(expected), new Bean(actual), path == null? "" : path + ".");
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	private static void assertEquals(Bean expected, Bean actual, String path) throws Throwable {
		
		Assertions.assertTrue(
				actual.getClassType().isAssignableFrom(expected.getClassType()), 
				actual.getClassType().getName() + " != " + expected.getClassType().getName());
		
		List<BeanPropertyAnnotation> props = expected.getProperties();
		
		for(BeanPropertyAnnotation expectedProperty: props) {
			
			BeanPropertyAnnotation actualProperty = (BeanPropertyAnnotation) actual.getProperty(expectedProperty.getName());
			
			if(expectedProperty.canSet()) {
				
				assertEquals(expected.get(expectedProperty.getName()), actual.get(actualProperty.getName()), path + expectedProperty.getName());
				
				if(expected.get(expectedProperty.getName()) == null) {
					if(actual.get(actualProperty.getName()) != null) {
						throw new AssertionFailedError(path + expectedProperty.getName() + " => null != " + actual.get(actualProperty.getName()));
					}
					else {
						continue;
					}
				}
				else
				if(actual.get(actualProperty.getName()) == null) {
					throw new AssertionFailedError(path + expectedProperty.getName() + " => " + actual.get(actualProperty.getName()) + " != null");
				}
				
				if(expectedProperty.getType().isAssignableFrom(Map.class)) {
					
					if(!actualProperty.getType().isAssignableFrom(Map.class)) {
						throw new AssertionFailedError(path + expectedProperty.getName() + " => " + expectedProperty.getType() + " " + actualProperty.getType());
					}
					
					try {
						assertEquals(
								(Map)expected.get(expectedProperty.getName()), 
								(Map)actual.get(actualProperty.getName()),
								path + expectedProperty.getName()
						);
					}
					catch(AssertionFailedError e) {
						throw new AssertionFailedError(path + expectedProperty.getName(), e);
					}
					
				}
				else
				if(expectedProperty.getType().isAssignableFrom(Collection.class)) {
					
					if(!actualProperty.getType().isAssignableFrom(Collection.class)) {
						throw new AssertionFailedError(path + expectedProperty.getName() + " => " + expectedProperty.getType() + " " + actualProperty.getType());
					}
					
					try {
						assertEquals(
								(Collection<?>)expected.get(expectedProperty.getName()), 
								(Collection<?>)actual.get(actualProperty.getName()),
								path + expectedProperty.getName()
						);
					}
					catch(AssertionFailedError e) {
						throw new AssertionFailedError(path + expectedProperty.getName(), e);
					}
					
				}
				else
				if(expectedProperty.isPrimitive()) {
					try {
						Assertions.assertEquals(
								expected.get(expectedProperty.getName()), 
								actual.get(actualProperty.getName())
						);
					}
					catch(AssertionFailedError e) {
						throw new AssertionFailedError(path + expectedProperty.getName(), e);
					}
				}
				else {
					
					assertEquals(
							expected.get(expectedProperty.getName()), 
							actual.get(actualProperty.getName()), 
							path + expectedProperty.getName()
					);
					
				}
			}
		}
	}
	
	private static void assertEquals(Collection<?> expected, Collection<?> actual, String path) throws Throwable {
		
        if (actual == null && expected == null) {
        	return;
        }
        
        if (actual == null || expected == null) {
        	throw new AssertionFailedError(expected + " != " + actual);
        }

        if (actual.isEmpty() && expected.isEmpty()) {
        	return;
        }
        
        Collection<?> a = new ArrayList<>(actual);
        Collection<?> e = new ArrayList<>(expected);

        Iterator<?> ei = e.iterator();
        int index = 0;
        
        while (ei.hasNext()) {
        	
            Object expectedItem = ei.next();
            Object found = null;
            
            for(Object actualItem: a) {
            	try {
            		assertEquals(expectedItem, actualItem, path + "[" + (index++) + "]");
            		found = actualItem;
            		break;
            	}
        		catch(AssertionFailedError ex) {
        			//ignore
        		}
            }
            
            if(found == null) {
            	throw new AssertionFailedError(expectedItem + " != null");
            }
            
            a.remove(found);
        }

    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void assertEquals(Map expected, Map actual, String path) throws Throwable {
		
        if (actual == null && expected == null) {
        	return;
        }
        
        if (actual == null || expected == null) {
        	throw new AssertionFailedError(expected + " != " + actual);
        }

        if (actual.isEmpty() && expected.isEmpty()) {
        	return;
        }
        
        Set<Entry> e = expected.entrySet();

        for (Entry expectedItem: e) {
        	
            Object actualItem = actual.get(expectedItem.getKey());
            
            if(actualItem != null) {
        		assertEquals(expectedItem.getValue(), actualItem, path + "[" + expectedItem.getKey() + "]");
            }
            else{
            	throw new AssertionFailedError(expectedItem + " != null");
            }
        }

    }
	
}
