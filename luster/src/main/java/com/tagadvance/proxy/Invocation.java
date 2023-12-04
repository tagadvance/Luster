package com.tagadvance.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public record Invocation<I>(Object proxy, Method method, I instance, Object[] args) {

	// TODO: unit test unrwap
	public Object invoke() throws Throwable {
		try {
			return method.invoke(instance, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
