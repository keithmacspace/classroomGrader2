package net.cdonald.googleClassroom.listenerCoordinator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingWorker;

//import net.cdonald.ListenerTest;
//import net.cdonald.LongQueryResponderTest;
//import net.cdonald.QueryResponseTest;
//import net.cdonald.ShortResponseTest;

public class ListenerCoordinator {
	private static Map<Class<?>, List<ListenerContainer>> listeners = new HashMap<Class<?>, List<ListenerContainer>>();	
	private static Map<Class<?>, Object> queryResponders = new HashMap<Class<?>, Object>();
	private static Map<Class<?>, LongQueryResponder<?>> longQueryResponders = new HashMap<Class<?>, LongQueryResponder<?>>();

	public static void addListener(Class<?> classToListenFor, Object listener) {
		addListener_(classToListenFor, new ListenerContainer(listener, true, false));
	}
	
	public static void addBlockingListener(Class<?> classToListenFor, Object listener) {
		addListener_(classToListenFor, new ListenerContainer(listener, true, true));
	}
	
	private static void addListener_(Class<?> classToListenFor, ListenerContainer listener) {
		checkForFired(classToListenFor);
		checkForFired(listener.getListener().getClass());
		//System.err.println("listener " + listener.toString());
		if (listeners.containsKey(classToListenFor) == false) {
			List<ListenerContainer> list = new ArrayList<ListenerContainer>();
			listeners.put(classToListenFor, list);
		}
		listeners.get(classToListenFor).add(listener);		
	}
	public static void disableListener(Class<?> classToListenFor, Object listener) {
		if (listeners.containsKey(classToListenFor) == true) {
			List<ListenerContainer> list = listeners.get(classToListenFor);			
			for (int i = list.size() - 1; i  > 0; i--) {

				ListenerContainer container = list.get(i);
				// Intentionally using == here, we are checking for a specific object
				if (container.getListener() == listener) {
					container.setEnabled(false);
				}
			}
		}
	}
	public static void enableListener(Class<?> classToListenFor, Object listener) {
		if (listeners.containsKey(classToListenFor) == true) {
			List<ListenerContainer> list = listeners.get(classToListenFor);
			for (int i = list.size() - 1; i  > 0; i--) {

				ListenerContainer container = list.get(i);
				// Intentionally using == here, we are checking for a specific object
				if (container.getListener() == listener) {
					container.setEnabled(true);
				}
			}
		}
	}

	
	private static void checkForFired(Class <?> classToCheck) {		
		boolean found = false;
		for (Method method : classToCheck.getMethods()) {
			if (method.getName().compareTo("fired") == 0) {
				found = true;
			}
		}
		if (found == false) {
			throw new IllegalArgumentException("Listener class " + classToCheck + " must have a fired method");
		}		
	}
	

	public static void addQueryResponder(Class<?> queryResponderClass, Object responder) {
		queryResponders.put(queryResponderClass, responder);		
	}

	public static void addLongQueryReponder(Class<?> queryResponderClass, LongQueryResponder<?> responder) {

		longQueryResponders.put(queryResponderClass, responder);
	}


	public static Object runQuery(Class<?> classToFire, Object param1, Object param2) {
		return runQuery_(classToFire,  new Object[] { param1, param2 });
	}

	public static Object runQuery(Class<?> classToFire, Object param) {
		return runQuery_(classToFire,  new Object[] { param });
	}

	public static Object runQuery(Class<?> classToFire) {
		return runQuery_(classToFire,  new Object[] {});
	}

	public static Object runQuery_(Class<?> classToFire, Object[] params) {
		if (queryResponders.containsKey(classToFire)) {
			Object object = queryResponders.get(classToFire);			
			return runOne(classToFire, "fired", object, params, createParamTypes(params));
		}
		return null;
	}
	
	public static void runLongQuery(Class<?> classToFire, LongQueryListener<?> listener) {
		if (longQueryResponders.containsKey(classToFire) == true) {			
			LongQueryResponder<?> responder = longQueryResponders.get(classToFire).newInstance();
			responder.setListener(listener);
			responder.execute();
		}
	}
	


	


	private static Class<?>[] createParamTypes(Object[] params) {
		Class<?>[] paramTypes = new Class<?>[params.length];
		for (int i = 0; i < params.length; i++) {
			if (params[i] == null) {
				paramTypes[i] = null;
			}
			else {
				paramTypes[i] = params[i].getClass();
			}
		}
		return paramTypes;
	}
		
	private static void runAll(Class<?> classToFire, Object[] params, boolean blockingValueToRun) {
		List<ListenerContainer> list = listeners.get(classToFire);
		Class<?>[] paramTypes = createParamTypes(params);
		if (list != null) {
			for (ListenerContainer listenerContainer : list) {
				if (listenerContainer.isEnabled() && listenerContainer.isBlocking() == blockingValueToRun) {		
					runOne(classToFire, "fired", listenerContainer.getListener(), params, paramTypes);
				}
			}
		}
	}
	public static void fire(Class<?> classToFire, Object param1, Object param2,
			Object param3, Object param4) {
		fire_(classToFire, new Object[] { param1, param2, param3, param4 });
	}
	
	public static void fire(Class<?> classToFire, Object param1, Object param2,
			Object param3) {
		fire_(classToFire, new Object[] { param1, param2, param3 });
	}

	public static void fire(Class<?> classToFire, Object param1, Object param2) {
		fire_(classToFire, new Object[] { param1, param2 });
	}

	public static void fire(Class<?> classToFire, Object param) {
		fire_(classToFire, new Object[] { param });
	}

	public static void fire(Class<?> classToFire) {
		fire_(classToFire, new Object[] {});
	}

	private static void fire_(Class<?> classToFire, Object[] params) {
		if (listeners.containsKey(classToFire) == true) {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					runAll(classToFire, params, false);
					return null;
				}
			};
			worker.execute();
		}
		runAll(classToFire, params, true);
	}

	private static Object runOne(Class<?> classToFire, String methodToFireName, Object objectToRun, Object[] params,
			Class<?>[] paramTypes) {

		Object returnValue = null;

		try {
			for (Method method : classToFire.getDeclaredMethods()) {
				if (method.getAnnotatedParameterTypes().length == paramTypes.length && method.getName().contentEquals(methodToFireName)) {
					return method.invoke(objectToRun, params);
				}
			}
			String paramString = "(";
			for (int i= 0; i < paramTypes.length; i++) {
				paramString += paramTypes[i].toString();
				if (i < paramTypes.length - 1) {
					paramString += ", ";
				}				
			}
			paramString += ")";
			
			System.err.println(classToFire.toString() + " should have a method fired" + paramString);
			System.err.println("Object we are using " + objectToRun.toString() +  " methods");
			for (Method method : objectToRun.getClass().getMethods()) {
				System.err.println(method.toGenericString());
			}
			return null;
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnValue;
	}
//
//	public static void main(String[] args) throws NoSuchMethodException, SecurityException {
//		
//		ListenerCoordinator.addQueryResponder(ShortResponseTest.class, new ShortResponseTest(3));
//		ListenerCoordinator.addLongQueryReponder(LongQueryResponderTest.class, new LongQueryResponderTest());
//		
//		QueryResponseTest responseListener = new QueryResponseTest();
//		ListenerCoordinator.runLongQuery(LongQueryResponderTest.class, responseListener);
//		System.out.println(ListenerCoordinator.runQuery(ShortResponseTest.class));
//		ListenerCoordinator.addListener(ListenerTest.class, new ListenerTest("Hi"));
//		ListenerCoordinator.addListener(ListenerTest.class, new ListenerTest("Bye"));
//		ListenerCoordinator.fire(ListenerTest.class, "method1");
//		ListenerCoordinator.fire(ListenerTest.class, "method2");
//		while (responseListener.isDone() == false)
//			;
//		
//		System.out.println("All done");
//
//	}
}
