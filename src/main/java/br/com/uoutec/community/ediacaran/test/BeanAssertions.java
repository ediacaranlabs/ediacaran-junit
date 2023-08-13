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
			assertBeanEquals(expected, actual, (e,a,p)->Boolean.TRUE);
		}
		catch(AssertionFailedError e) {
			throw e;
		}
		catch(Throwable e) {
			throw new AssertionFailedError("unexpected error", e);
		}
	}

	public static void assertBeanEquals(Object expected, Object actual, BeanAssertionsFilter filter) {
		try {
			assertEquals(expected, actual, filter, null);
		}
		catch(AssertionFailedError e) {
			throw e;
		}
		catch(Throwable e) {
			throw new AssertionFailedError("unexpected error", e);
		}
	}
	
	private static void assertEquals(Object expected, Object actual, BeanAssertionsFilter filter, String path) throws Throwable {
		
		if(!filter.isTest(expected, actual, path)) {
			return;
		}
		
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
		if(expected instanceof Map) {
			
			if(!(actual instanceof Map)) {
				throw new AssertionFailedError(path  + " => " + expected.getClass() + " " + actual.getClass());
			}
			
			assertEquals((Map<?,?>)expected, (Map<?,?>)actual, filter, path);
			
		}
		else
		if(expected instanceof Collection) {
			
			if(!(actual instanceof Collection)) {
				throw new AssertionFailedError(path + " => " + expected.getClass() + " " + actual.getClass());
			}
			
			assertEquals((Collection<?>)expected, (Collection<?>)actual, filter, path);
		}		
		else
		if(Bean.isPrimitive(expected.getClass())) {
			Assertions.assertEquals(expected, actual);
		}
		else {
			assertEquals(new Bean(expected), new Bean(actual), filter, path == null? "" : path + ".");
		}
		
	}
	
	private static void assertEquals(Bean expected, Bean actual, BeanAssertionsFilter filter, String path) throws Throwable {
		
		Assertions.assertTrue(
				actual.getClassType().isAssignableFrom(expected.getClassType()), 
				actual.getClassType().getName() + " != " + expected.getClassType().getName());
		
		List<BeanPropertyAnnotation> props = expected.getProperties();
		
		for(BeanPropertyAnnotation expectedProperty: props) {
			
			BeanPropertyAnnotation actualProperty = (BeanPropertyAnnotation) actual.getProperty(expectedProperty.getName());
			
			if(expectedProperty.canSet()) {

				try {
					assertEquals(expected.get(expectedProperty.getName()), 
							actual.get(actualProperty.getName()), filter, path + expectedProperty.getName());
				}
				catch(AssertionFailedError e) {
					throw new AssertionFailedError(path + expectedProperty.getName(), e);
				}
				

			}
		}
	}
	
	private static void assertEquals(Collection<?> expected, Collection<?> actual, BeanAssertionsFilter filter, String path) throws Throwable {
		
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
            		assertEquals(expectedItem, actualItem, filter, path + "[" + (index++) + "]");
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
	private static void assertEquals(Map expected, Map actual, BeanAssertionsFilter filter, String path) throws Throwable {
		
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
        		assertEquals(expectedItem.getValue(), actualItem, filter, path + "[" + expectedItem.getKey() + "]");
            }
            else{
            	throw new AssertionFailedError(expectedItem + " != null");
            }
        }

    }

	@FunctionalInterface
	public static interface BeanAssertionsFilter {
		
		boolean isTest(Object expected, Object actual, String path);
		
	}
	
}
