package com.tagadvance.exception;

import java.util.concurrent.Callable;

/**
 * A {@link Callable} that throws an exception of type {@link E}.
 *
 * @param <V> the result type of method {@link #call()}
 * @param <E> the type of exception that may be thrown by {@link #call()}
 */
@FunctionalInterface
public interface CheckedCallable<V, E extends Exception> extends Callable<V> {

	/**
	 * Computes a result, or throws an exception if unable to do so.
	 *
	 * @return computed result
	 * @throws E if unable to compute a result
	 */
	V call() throws E;

}
