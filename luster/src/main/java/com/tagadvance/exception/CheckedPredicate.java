package com.tagadvance.exception;

import java.util.function.Predicate;

/**
 * A {@link Predicate} that erases the checked-ness of {@link E} so the predicate can be handed to
 * the JDK, e.g. {@link java.util.stream.Stream#filter(Predicate)}.
 * <p>
 * Checked exceptions are rethrown as {@link UncheckedException}; unchecked exceptions propagate
 * unchanged.
 *
 * @param <I> the type of the input to the predicate
 * @param <E> the type of exception that may be thrown by this predicate
 */
@FunctionalInterface
public interface CheckedPredicate<I, E extends Exception> extends Predicate<I>,
	ThrowingPredicate<I, E> {

	@Override
	default boolean test(final I i) throws UncheckedException {
		try {
			return testChecked(i);
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new UncheckedException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedPredicate} in a {@link Predicate} that
	 * automatically re-throws checked exceptions as {@link UncheckedException}.
	 *
	 * @param predicate a {@link CheckedPredicate}
	 * @param <I>       the type of the input
	 * @param <E>       the type of exception that may be thrown
	 * @return the {@link Predicate} wrapper
	 */
	static <I, E extends Exception> Predicate<I> of(final CheckedPredicate<I, E> predicate) {
		return predicate;
	}

}
