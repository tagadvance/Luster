package com.tagadvance.exception;

import java.util.function.Supplier;

/**
 * A {@link Supplier} that propagates an exception of type {@link E} to its caller.
 *
 * @param <V> the type of result supplied
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingSupplier<V, E extends Exception> {

	/**
	 * Gets a result.
	 *
	 * @return a result
	 * @throws E the type of exception that may be thrown
	 */
	V get() throws E;

}
