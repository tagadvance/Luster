package com.tagadvance.exception;

import java.util.function.Function;

/**
 * A {@link Function} that throws an exception of type {@link E}.
 *
 * @param <I> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception that may be thrown by this function
 */
@FunctionalInterface
public interface CheckedFunction<I, R, E extends Exception> extends Function<I, R> {

	/**
	 * This method is like {@link Function#apply(Object)}} except that it may throw an exception of
	 * type {@link E}.
	 *
	 * @param i the type of the input
	 * @return the function result
	 * @throws E the type of exception that may be thrown
	 */
	R applyChecked(I i) throws E;

	@Override
	default R apply(final I i) throws UncheckedExecutionException {
		try {
			return applyChecked(i);
		} catch (final Exception e) {
			throw new UncheckedExecutionException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedFunction} in a {@link Function} that
	 * automatically re-throws checked exceptions as {@link UncheckedExecutionException}.
	 *
	 * @param function a {@link CheckedFunction}
	 * @param <T>      the type of the input
	 * @param <R>      the type of the result
	 * @param <E>      the type of exception that may be thrown
	 * @return the {@link Function} wrapper
	 */
	static <T, R, E extends Exception> Function<T, R> of(final CheckedFunction<T, R, E> function) {
		return function;
	}

}
