package com.tagadvance.exception;

import java.util.function.Function;

/**
 * A {@link Function} that erases the checked-ness of {@link E} so the function can be handed to
 * the JDK, e.g. {@link java.util.stream.Stream#map(Function)}.
 * <p>
 * Checked exceptions are rethrown as {@link UncheckedException}; unchecked exceptions propagate
 * unchanged.
 *
 * @param <I> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception that may be thrown by this function
 */
@FunctionalInterface
public interface CheckedFunction<I, R, E extends Exception> extends Function<I, R>,
	ThrowingFunction<I, R, E> {

	@Override
	default R apply(final I i) throws UncheckedException {
		try {
			return applyChecked(i);
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new UncheckedException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedFunction} in a {@link Function} that
	 * automatically re-throws checked exceptions as {@link UncheckedException}.
	 *
	 * @param function a {@link CheckedFunction}
	 * @param <I>      the type of the input
	 * @param <R>      the type of the result
	 * @param <E>      the type of exception that may be thrown
	 * @return the {@link Function} wrapper
	 */
	static <I, R, E extends Exception> Function<I, R> of(final CheckedFunction<I, R, E> function) {
		return function;
	}

}
