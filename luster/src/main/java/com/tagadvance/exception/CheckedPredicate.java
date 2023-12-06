package com.tagadvance.exception;

import java.util.function.Predicate;

/**
 * A {@link Predicate} that throws an exception of type {@link T}.
 *
 * @param <I> the type of the input to the predicate
 * @param <T> the type of exception that may be thrown by this predicate
 */
@FunctionalInterface
public interface CheckedPredicate<I, T extends Throwable> extends Predicate<I> {

	/**
	 * This method is like {@link Predicate#test(Object)}} except that it may throw an exception of
	 * type {@link T}.
	 *
	 * @param i the input argument
	 * @return {@literal true} if the input argument matches the predicate, otherwise
	 * {@literal false}
	 * @throws T the type of exception that may be thrown
	 */
	boolean testChecked(I i) throws T;

	@Override
	default boolean test(I i) throws UncheckedExecutionException {
		try {
			return testChecked(i);
		} catch (final Throwable t) {
			throw new UncheckedExecutionException(t);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedPredicate} in a {@link Predicate} that
	 * automatically re-throws checked exceptions as {@link UncheckedExecutionException}.
	 *
	 * @param predicate a {@link CheckedPredicate}
	 * @param <T>       the type of the input
	 * @param <E>       the type of exception that may be thrown
	 * @return the {@link Predicate} wrapper
	 */
	static <T, E extends Throwable> Predicate<T> of(final CheckedPredicate<T, E> predicate) {
		return predicate;
	}

}
