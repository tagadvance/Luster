package com.tagadvance.exception;

/**
 * A function that propagates an exception of type {@link E} to its caller.
 *
 * @param <I> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingFunction<I, R, E extends Exception> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param i the input argument
	 * @return the function result
	 * @throws E the type of exception that may be thrown
	 */
	R applyChecked(I i) throws E;

}
