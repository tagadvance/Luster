package com.tagadvance.exception;

/**
 * A {@link Runnable} that erases the checked-ness of {@link E} so the runnable can be handed to
 * the JDK, e.g. {@link java.util.concurrent.ExecutorService#submit(Runnable)}.
 * <p>
 * Checked exceptions are rethrown as {@link UncheckedException}; unchecked exceptions propagate
 * unchanged.
 *
 * @param <E> the type of exception that may be thrown by this runnable
 */
@FunctionalInterface
public interface CheckedRunnable<E extends Exception> extends Runnable, ThrowingRunnable<E> {

	@Override
	default void run() throws UncheckedException {
		try {
			runChecked();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new UncheckedException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedRunnable} in a {@link Runnable} that
	 * automatically re-throws checked exceptions as {@link UncheckedException}.
	 *
	 * @param runnable a {@link CheckedRunnable}
	 * @param <E>      the type of exception that may be thrown
	 * @return the {@link Runnable} wrapper
	 */
	static <E extends Exception> Runnable of(final CheckedRunnable<E> runnable) {
		return runnable;
	}

}
