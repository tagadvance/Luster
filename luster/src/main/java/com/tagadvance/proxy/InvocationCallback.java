package com.tagadvance.proxy;

import java.lang.reflect.Method;

/**
 * A callback around {@link Method#invoke(Object, Object...) method invocation}.
 */
@FunctionalInterface
public interface InvocationCallback {

	/**
	 * This is a callback for a method invocation. It is the implementor's responsibility to decide
	 * if and how to process the invocation. The result must conform to the original method
	 * contract!
	 *
	 * @param invocation an {@link Invocation invocation}
	 * @return the result
	 * @throws Throwable It could be anything. Who knows? The Lord knows.
	 */
	Object onInvocation(final Invocation invocation) throws Throwable;

}
