package com.tagadvance.utilities;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Sleep utility.
 */
@FunctionalInterface
public interface Sleep {

	/**
	 *
	 * @param timeout the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                all.
	 * @return {@link Boolean#TRUE true} if the {@link Thread#sleep(long) sleep} operation finished
	 * successfully or {@link Boolean#FALSE false} if it was {@link Thread#interrupt() interrupted}
	 */
	default boolean slept(final Duration timeout) {
		try {
			sleep(timeout);

			return true;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();

			return false;
		}
	}

	/**
	 * @param timeout  the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                 all.
	 * @param timeUnit the {@link TimeUnit time unit} of the timeout parameter
	 * @return {@link Boolean#TRUE true} if the {@link Thread#sleep(long) sleep} operation finished
	 * successfully or {@link Boolean#FALSE false} if it was {@link Thread#interrupt() interrupted}
	 */
	default boolean slept(final long timeout, TimeUnit timeUnit) {
		try {
			sleep(timeout, timeUnit);

			return true;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();

			return false;
		}
	}

	/**
	 * @param milliseconds the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                     all.
	 * @return {@link Boolean#TRUE true} if the {@link Thread#sleep(long) sleep} operation finished
	 * successfully or {@link Boolean#FALSE false} if it was {@link Thread#interrupt() interrupted}
	 */
	default boolean slept(final int milliseconds) {
		try {
			sleep(milliseconds);

			return true;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();

			return false;
		}
	}

	/**
	 *
	 * @param timeout the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                all.
	 * @throws InterruptedException if interrupted while sleeping
	 */
	default void sleep(final Duration timeout) throws InterruptedException {
		sleep(timeout.toNanos(), TimeUnit.NANOSECONDS);
	}

	/**
	 *
	 * @param timeout  the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                 all.
	 * @param timeUnit the {@link TimeUnit time unit} of the timeout parameter
	 * @throws InterruptedException if interrupted while sleeping
	 */
	default void sleep(final long timeout, TimeUnit timeUnit) throws InterruptedException {
		final var millis = timeUnit.toMillis(timeout);

		sleep(millis);
	}

	/**
	 *
	 * @param milliseconds the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                     all.
	 * @throws InterruptedException if interrupted while sleeping
	 */
	void sleep(final long milliseconds) throws InterruptedException;

}
