package com.tagadvance.exception;

/**
 * A consumer that propagates an exception of type {@link E} to its caller.
 *
 * @param <I> the type of the input to the operation
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingConsumer<I, E extends Exception> {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param i the input argument
	 * @throws E the type of exception that may be thrown
	 */
	void acceptChecked(I i) throws E;

}
