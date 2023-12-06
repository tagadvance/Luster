package com.tagadvance.exception;

import java.util.function.Consumer;

/**
 * A {@link Consumer} that throws an exception of type {@link T}.
 *
 * @param <I> the type of the input to the operation
 * @param <T> the type of exception that may be thrown by this consumer
 */
@FunctionalInterface
public interface CheckedConsumer<I, T extends Throwable> extends Consumer<I> {

	/**
	 * This method is like {@link Consumer#accept(Object)} except that it may throw an exception of
	 * type {@link T}.
	 *
	 * @param i the input argument
	 * @throws T the type of exception that may be thrown
	 */
	void acceptChecked(I i) throws T;

	@Override
	default void accept(I i) throws UncheckedExecutionException {
		try {
			acceptChecked(i);
		} catch (final Throwable T) {
			throw new UncheckedExecutionException(T);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedConsumer} in a {@link Consumer} that
	 * automatically re-throws checked exceptions as {@link UncheckedExecutionException}.
	 *
	 * @param consumer a {@link CheckedConsumer}
	 * @param <I>      the type of the input to the operation
	 * @param <T>      the type of exception that may be thrown
	 * @return the {@link Consumer} wrapper
	 */
	static <I, T extends Throwable> Consumer<I> of(final CheckedConsumer<I, T> consumer) {
		return consumer;
	}

}
