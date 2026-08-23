package com.tagadvance.exception;

import java.util.concurrent.Callable;

/**
 * A {@link Callable} that propagates an exception of type {@link E} to its caller.
 * <p>
 * Deliberately does not extend {@link Callable}: {@link Callable#call()} declares
 * {@code throws Exception}, which erases {@link E} for callers that need it in their own
 * signature.
 *
 * @param <V> the result type of {@link #call()}
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingCallable<V, E extends Exception> {

	/**
	 * Computes a result, or throws an exception if unable to do so.
	 *
	 * @return the computed result
	 * @throws E if unable to compute a result
	 */
	V call() throws E;

}
