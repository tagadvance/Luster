package com.tagadvance.proxy;

import java.lang.reflect.Method;

public record Invocation(Object proxy, Method method, Object instance, Object[] args) {

	// TODO: unit test unrwap
	public Object call() throws Throwable {
		if (!method.canAccess(instance)) {
			method.trySetAccessible();
		}

		return method.invoke(instance, args);
	}

}
