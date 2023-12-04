package com.tagadvance.exception;

import java.util.function.Predicate;

/**
 * A {@link Predicate} that throws an exception of type {@link E}.
 *
 * @param <T> the type of the input to the predicate
 * @param <E> the type of exception that may be thrown by this predicate
 */
@FunctionalInterface
public interface CheckedPredicate<T, E extends Exception> extends Predicate<T> {

	/**
	 * This method is like {@link Predicate#test(Object)}} except that it may throw an exception of
	 * type {@link E}.
	 *
	 * @param t the input argument
	 * @return {@literal true} if the input argument matches the predicate, otherwise
	 * {@literal false}
	 * @throws E the type of exception that may be thrown
	 */
	boolean testChecked(T t) throws E;

	@Override
	default boolean test(T t) throws UncheckedExecutionException {
		try {
			return testChecked(t);
		} catch (final Exception e) {
			throw new UncheckedExecutionException(e);
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
	static <T, E extends Exception> Predicate<T> of(final CheckedPredicate<T, E> predicate) {
		return predicate::test;
	}

}
