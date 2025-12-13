package com.tagadvance.exception;

import java.util.Comparator;

/**
 * A {@link Comparator} that throws an exception of type {@link E}.
 *
 * @param <I> the type of objects that may be compared by this comparator
 * @param <E> the type of exception that may be thrown by this comparator
 */
@FunctionalInterface
public interface CheckedComparator<I, E extends Exception> extends Comparator<I> {

	/**
	 * This method is like {@link Comparator#compare(Object, Object)} except that it may throw an
	 * exception of type {@link E}.
	 *
	 * @param o1 the first object to be compared
	 * @param o2 the second object to be compared
	 * @return a negative integer, zero, or a positive integer as the first argument is less than,
	 * equal to, or greater than the second
	 * @throws E the type of exception
	 */
	int compareChecked(I o1, I o2) throws E;

	@Override
	default int compare(final I o1, I o2) throws UncheckedExecutionException {
		try {
			return compareChecked(o1, o2);
		} catch (final Exception e) {
			throw new UncheckedExecutionException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedComparator} in a {@link Comparator} that
	 * automatically re-throws checked exceptions as {@link UncheckedExecutionException}.
	 *
	 * @param comparator a {@link CheckedComparator}
	 * @param <I>        the type of objects that may be compared
	 * @param <E>        the type of exception that may be thrown
	 * @return the {@link Comparator} wrapper
	 */
	static <I, E extends Exception> Comparator<I> of(final CheckedComparator<I, E> comparator) {
		return comparator;
	}

}
