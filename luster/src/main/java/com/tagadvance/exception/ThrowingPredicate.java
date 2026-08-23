package com.tagadvance.exception;

/**
 * A predicate that propagates an exception of type {@link E} to its caller.
 *
 * @param <I> the type of the input to the predicate
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingPredicate<I, E extends Exception> {

	/**
	 * Evaluates this predicate on the given argument.
	 *
	 * @param i the input argument
	 * @return {@literal true} if the input argument matches the predicate, otherwise
	 * {@literal false}
	 * @throws E the type of exception that may be thrown
	 */
	boolean testChecked(I i) throws E;

}
