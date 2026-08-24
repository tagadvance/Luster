package com.tagadvance.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class CacheUtils {

	/**
	 * Invokes {@literal method}, unwrapping the {@link InvocationTargetException} so that what the
	 * real method threw is what propagates.
	 */
	static Object invoke(final Method method, final Object instance, final Object[] args)
		throws Throwable {
		if (!method.canAccess(instance)) {
			method.trySetAccessible();
		}

		try {
			return method.invoke(instance, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private CacheUtils() {
	}

}
