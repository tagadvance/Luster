package com.tagadvance.exception;

/**
 * A {@link Runnable} that throws an exception of type {@link E}.
 *
 * @param <E> the type of exception that may be thrown by this runnable
 */
@FunctionalInterface
public interface CheckedRunnable<E extends Exception> extends Runnable {

	/**
	 * This method is like {@link Runnable#run()} except that it may throw an exception of type
	 * {@link E}.
	 *
	 * @throws E the type of exception that may be thrown
	 */
	void runChecked() throws E;

	@Override
	default void run() throws UncheckedExecutionException {
		try {
			runChecked();
		} catch (final Exception e) {
			throw new UncheckedExecutionException(e);
		}
	}

	/**
	 * This method wraps the supplied {@link CheckedRunnable} in a {@link Runnable} that
	 * automatically re-throws checked exceptions as {@link UncheckedExecutionException}.
	 *
	 * @param runnable a {@link CheckedRunnable}
	 * @param <E>      the type of exception that may be thrown
	 * @return the {@link Runnable} wrapper
	 */
	static <E extends Exception> Runnable of(final CheckedRunnable<E> runnable) {
		return runnable::run;
	}

}
