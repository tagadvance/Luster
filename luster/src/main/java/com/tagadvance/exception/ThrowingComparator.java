package com.tagadvance.exception;

/**
 * A comparator that propagates an exception of type {@link E} to its caller.
 *
 * @param <I> the type of objects that may be compared
 * @param <E> the type of exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingComparator<I, E extends Exception> {

	/**
	 * Compares its two arguments for order.
	 *
	 * @param o1 the first object to be compared
	 * @param o2 the second object to be compared
	 * @return a negative integer, zero, or a positive integer as the first argument is less than,
	 * equal to, or greater than the second
	 * @throws E the type of exception that may be thrown
	 */
	int compareChecked(I o1, I o2) throws E;

}
