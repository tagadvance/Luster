package com.tagadvance.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents a {@link Method#invoke(Object, Object...) method invocation}.
 *
 * @param proxy    a proxy instance
 * @param method   the {@link Method method} to {@link Method#invoke(Object, Object...) invoke}
 * @param instance the object the method is invoked from
 * @param args     the arguments supplied to the method
 */
public record Invocation(Object proxy, Method method, Object instance, Object... args) {

	/**
	 * This is essentially the same as calling <code>instance.method(args);</code>
	 * <p>
	 * {@link IllegalAccessException} is unlikely to be thrown as, if necessary, the
	 * {@link Method method} will automatically set to {@literal accessible}.
	 * <p>
	 * {@link InvocationTargetException}, if caught, is automatically unwrapped and the
	 * {@link Throwable#getCause() cause} will be thrown instead.
	 *
	 * @return the result of the {@link Method#invoke(Object, Object...) method invocation}
	 * @throws Throwable It could be anything. Who knows? The Lord knows.
	 * @see Method#invoke(Object, Object...)
	 */
	public Object invoke() throws Throwable {
		if (!method.canAccess(instance)) {
			method.trySetAccessible();
		}

		try {
			return method.invoke(instance, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
