package com.tagadvance.utilities;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * An injectable abstraction over {@link Thread#sleep(long, int)}.
 *
 * <p>Before reaching for this, consider whether the sleep is necessary at all. A sleep in the
 * middle of business logic is usually a symptom of polling or busy-waiting, and the old warning
 * about both still holds: a sleep long enough to be cheap is too slow, and a sleep short enough to
 * be responsive burns a thread for nothing. The duration is a guess about someone else's timing,
 * and guesses of that sort are what make a test suite flaky.</p>
 *
 * <p>Where the API being waited on offers an asynchronous variant, a callback, a listener or a
 * completion hook, use that instead. Where a {@link java.util.concurrent.ScheduledExecutorService}
 * is already in play - this library's {@code cache}, {@code debounce} and {@code logging} packages
 * all accept one - inject a test scheduler rather than a test {@link Sleep}: a scheduler gives
 * deterministic control over every delay in the component, not just the ones that happen to be
 * routed through here.</p>
 *
 * <p>{@link Sleep} earns its place in retry and backoff loops, where there is no scheduler to hand
 * and the upstream API offers nothing better than trying again later. Sometimes a sleep genuinely
 * is the only option; when it is, injecting one keeps the waiting out of the tests.</p>
 *
 * <p>Implementations are supplied by {@link #ofThread()}, {@link #disabled()},
 * {@link #uninterruptible(Sleep)} and {@link #recorder()}.</p>
 */
@FunctionalInterface
public interface Sleep {

	/**
	 * Sleep for at least the supplied duration.
	 *
	 * @param timeout the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                all.
	 * @throws InterruptedException if interrupted while sleeping
	 */
	void sleep(Duration timeout) throws InterruptedException;

	/**
	 * Sleep for at least the supplied timeout.
	 *
	 * @param timeout  the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                 all.
	 * @param timeUnit the {@link TimeUnit time unit} of the timeout parameter
	 * @throws InterruptedException if interrupted while sleeping
	 */
	default void sleep(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		requireNonNull(timeUnit, "timeUnit must not be null");

		sleep(Duration.of(timeout, timeUnit.toChronoUnit()));
	}

	/**
	 * Sleep for at least the supplied duration, restoring the interrupt status instead of throwing
	 * if interrupted.
	 *
	 * @param timeout the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                all.
	 * @return {@link Boolean#TRUE true} if the sleep finished successfully or
	 * {@link Boolean#FALSE false} if it was {@link Thread#interrupt() interrupted}
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
	 * Sleep for at least the supplied timeout, restoring the interrupt status instead of throwing
	 * if interrupted.
	 *
	 * @param timeout  the minimum time to sleep. If less than or equal to zero, do not sleep at
	 *                 all.
	 * @param timeUnit the {@link TimeUnit time unit} of the timeout parameter
	 * @return {@link Boolean#TRUE true} if the sleep finished successfully or
	 * {@link Boolean#FALSE false} if it was {@link Thread#interrupt() interrupted}
	 */
	default boolean slept(final long timeout, final TimeUnit timeUnit) {
		try {
			sleep(timeout, timeUnit);

			return true;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();

			return false;
		}
	}

	/**
	 * @return a {@link Sleep} backed by {@link Thread#sleep(long, int)}
	 */
	static Sleep ofThread() {
		return timeout -> {
			if (timeout.isZero() || timeout.isNegative()) {
				return;
			}

			// toNanosPart may exceed the sub-millisecond range Thread#sleep accepts
			Thread.sleep(timeout.toMillis(), timeout.toNanosPart() % 1_000_000);
		};
	}

	/**
	 * @return a {@link Sleep} that returns immediately, the sane default for tests
	 */
	static Sleep disabled() {
		return timeout -> {

		};
	}

	/**
	 * Decorate the supplied {@link Sleep} so that an interrupt does not cut the sleep short. The
	 * interrupt is swallowed for the remainder of the sleep and re-asserted on the way out, so the
	 * caller still observes it at its next interruptible operation.
	 *
	 * @param sleep the {@link Sleep} to decorate
	 * @return the decorated {@link Sleep}
	 */
	static Sleep uninterruptible(final Sleep sleep) {
		requireNonNull(sleep, "sleep must not be null");

		return timeout -> {
			final var deadline = System.nanoTime() + Math.max(0, timeout.toNanos());

			var remaining = timeout;
			var interrupted = false;
			try {
				while (true) {
					try {
						sleep.sleep(remaining);

						break;
					} catch (final InterruptedException e) {
						interrupted = true;
						remaining = Duration.ofNanos(deadline - System.nanoTime());
						if (remaining.isZero() || remaining.isNegative()) {
							break;
						}
					}
				}
			} finally {
				if (interrupted) {
					Thread.currentThread().interrupt();
				}
			}
		};
	}

	/**
	 * @return a new {@link Recorder}
	 */
	static Recorder recorder() {
		return new Recorder();
	}

	/**
	 * A {@link Sleep} that records what it was asked to sleep for and returns immediately. It lets
	 * a test assert that, say, a retry loop backed off by one, two and then four seconds without
	 * spending seven seconds proving it.
	 */
	final class Recorder implements Sleep {

		private final List<Duration> durations = new CopyOnWriteArrayList<>();

		private Recorder() {

		}

		@Override
		public void sleep(final Duration timeout) {
			durations.add(requireNonNull(timeout, "timeout must not be null"));
		}

		/**
		 * @return an immutable {@link List} of every duration this {@link Sleep} was asked for, in
		 * the order it was asked
		 */
		public List<Duration> durations() {
			return List.copyOf(durations);
		}

		/**
		 * Discard the recorded durations.
		 */
		public void reset() {
			durations.clear();
		}

	}

}
