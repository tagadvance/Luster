package com.tagadvance.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

public record Invocation(Object proxy, Method method, Object instance, Object[] args) {

	// TODO: unit test unrwap
	public Object call() throws Throwable {
		try {
			return method.invoke(instance, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
