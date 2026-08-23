package com.tagadvance.exception;

/**
 * A {@link Runnable} that propagates an exception of type {@link E} to its caller.
 *
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {

	/**
	 * Performs this operation.
	 *
	 * @throws E the type of exception that may be thrown
	 */
	void runChecked() throws E;

}
