package com.tagadvance.exception;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * A {@link Supplier} that propagates an exception of type {@link E} to its caller.
 * <p>
 * This also covers what a {@link Callable} would do. {@link Callable#call()} declares
 * {@code throws Exception}, which erases {@link E} for callers that need it in their own
 * signature, so there is no separate throwing counterpart for it.
 *
 * @param <V> the type of result supplied
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingSupplier<V, E extends Exception> {

	/**
	 * Gets a result, or throws an exception if unable to do so.
	 *
	 * @return a result
	 * @throws E if unable to supply a result
	 */
	V get() throws E;

}
