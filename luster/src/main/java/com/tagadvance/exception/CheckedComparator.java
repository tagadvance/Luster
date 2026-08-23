package com.tagadvance.exception;

import java.util.Comparator;

/**
 * A {@link Comparator} that erases the checked-ness of {@link E} so the comparator can be handed
 * to the JDK, e.g. {@link java.util.stream.Stream#sorted(Comparator)}.
 * <p>
 * Checked exceptions are rethrown as {@link UncheckedException}; unchecked exceptions propagate
 * unchanged.
 *
 * @param <I> the type of objects that may be compared by this comparator
 * @param <E> the type of exception that may be thrown by this comparator
 */
@FunctionalInterface
public interface CheckedComparator<I, E extends Exception> extends Comparator<I>,
	ThrowingComparator<I, E> {

	@Override
	default int compare(final I o1, final I o2) throws UncheckedException {
		try {
			return compareChecked(o1, o2);
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new UncheckedException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedComparator} in a {@link Comparator} that
	 * automatically re-throws checked exceptions as {@link UncheckedException}.
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
