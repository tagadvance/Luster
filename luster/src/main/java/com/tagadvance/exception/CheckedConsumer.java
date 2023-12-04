package com.tagadvance.exception;

import java.util.function.Consumer;

/**
 * A {@link Consumer} that throws an exception of type {@link E}.
 *
 * @param <T> the type of the input to the operation
 * @param <E> the type of exception that may be thrown by this consumer
 */
@FunctionalInterface
public interface CheckedConsumer<T, E extends Exception> extends Consumer<T> {

	/**
	 * This method is like {@link Consumer#accept(Object)} except that it may throw an exception of
	 * type {@link E}.
	 *
	 * @param t the input argument
	 * @throws E the type of exception that may be thrown
	 */
	void acceptChecked(T t) throws E;

	@Override
	default void accept(T t) throws UncheckedExecutionException {
		try {
			acceptChecked(t);
		} catch (final Exception e) {
			throw new UncheckedExecutionException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedConsumer} in a {@link Consumer} that
	 * automatically re-throws checked exceptions as {@link UncheckedExecutionException}.
	 *
	 * @param consumer a {@link CheckedConsumer}
	 * @param <T>      the type of the input to the operation
	 * @param <E>      the type of exception that may be thrown
	 * @return the {@link Consumer} wrapper
	 */
	static <T, E extends Exception> Consumer<T> of(final CheckedConsumer<T, E> consumer) {
		return consumer::accept;
	}

}
