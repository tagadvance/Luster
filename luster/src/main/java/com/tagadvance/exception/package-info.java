/**
 * Two families of functional interface, for two different jobs.
 * <p>
 * <b>Adapters</b> — {@code CheckedFunction}, {@code CheckedConsumer}, {@code CheckedPredicate},
 * {@code CheckedRunnable}, {@code CheckedComparator}. These extend the JDK type so they can be
 * handed to it, and rethrow checked exceptions as {@link com.tagadvance.exception.UncheckedException}
 * because the JDK signature forbids anything else. Unchecked exceptions propagate unchanged.
 * Restore the original exception at the boundary with
 * {@link com.tagadvance.exception.Checked#rethrowing}.
 * <p>
 * <b>Throwing types</b> — {@code ThrowingRunnable}, {@code ThrowingConsumer},
 * {@code ThrowingFunction}, {@code ThrowingPredicate}, {@code ThrowingComparator},
 * {@code ThrowingSupplier}. These extend nothing and propagate
 * {@code E} honestly, so a method taking one can declare {@code throws E} and have it mean
 * something. Use them as parameter types in your own API.
 * <p>
 * Each adapter extends its throwing counterpart, so anything that accepts a throwing type also
 * accepts the matching adapter. That is why the throwing types name their method
 * {@code applyChecked}, {@code acceptChecked}, and so on — the plain name is taken by the JDK
 * interface the adapter also implements. {@code ThrowingSupplier} has no adapter counterpart and
 * so keeps the plain {@code get}; it also stands in for a throwing {@code Callable}, since
 * {@code Callable.call()} declares {@code throws Exception} and would erase {@code E}.
 */
package com.tagadvance.exception;
