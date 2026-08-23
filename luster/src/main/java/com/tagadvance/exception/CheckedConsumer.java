package com.tagadvance.exception;

import java.util.function.Consumer;

/**
 * A {@link Consumer} that erases the checked-ness of {@link E} so the consumer can be handed to
 * the JDK, e.g. {@link java.util.stream.Stream#forEach(Consumer)}.
 * <p>
 * Checked exceptions are rethrown as {@link UncheckedException}; unchecked exceptions propagate
 * unchanged.
 *
 * @param <I> the type of the input to the operation
 * @param <E> the type of exception that may be thrown by this consumer
 */
@FunctionalInterface
public interface CheckedConsumer<I, E extends Exception> extends Consumer<I>,
	ThrowingConsumer<I, E> {

	@Override
	default void accept(final I i) throws UncheckedException {
		try {
			acceptChecked(i);
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new UncheckedException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedConsumer} in a {@link Consumer} that
	 * automatically re-throws checked exceptions as {@link UncheckedException}.
	 *
	 * @param consumer a {@link CheckedConsumer}
	 * @param <I>      the type of the input to the operation
	 * @param <E>      the type of exception that may be thrown
	 * @return the {@link Consumer} wrapper
	 */
	static <I, E extends Exception> Consumer<I> of(final CheckedConsumer<I, E> consumer) {
		return consumer;
	}

}
