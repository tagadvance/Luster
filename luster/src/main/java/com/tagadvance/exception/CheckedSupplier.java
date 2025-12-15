package com.tagadvance.exception;

import java.util.function.Supplier;

/**
 * Alternative to {@link Supplier} that allows for checked exceptions.
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Exception> {

	T get() throws E;

}
