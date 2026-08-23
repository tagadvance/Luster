package com.tagadvance.debounce;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

final class TrailingDebouncer implements Debouncer {

	private final ScheduledExecutorService scheduler;

	private final Duration delay;

	private final @Nullable Duration maxWait;

	private final Runnable callback;

	/**
	 * The state below is small but has to move together, and the transitions schedule and cancel
	 * futures — which is exactly what an {@link java.util.concurrent.atomic.AtomicReference}
	 * update function must not do, since it is re-applied on CAS failure.
	 */
	private final Lock lock = new ReentrantLock();

	private @Nullable ScheduledFuture<?> delayFuture;

	private @Nullable ScheduledFuture<?> maxWaitFuture;

	private boolean pending;

	TrailingDebouncer(final ScheduledExecutorService scheduler, final Duration delay,
		final @Nullable Duration maxWait, final Runnable callback) {
		this.scheduler = requireNonNull(scheduler, "scheduler must not be null");
		this.delay = requirePositive(delay, "delay");
		this.maxWait = maxWait == null ? null : requirePositive(maxWait, "maxWait");
		this.callback = requireNonNull(callback, "callback must not be null");
	}

	private static Duration requirePositive(final Duration duration, final String name) {
		requireNonNull(duration, "%s must not be null".formatted(name));
		if (duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException("%s must be positive: %s".formatted(name, duration));
		}

		return duration;
	}

	@Override
	public Debouncer withMaxWait(final Duration maxWait) {
		return new TrailingDebouncer(scheduler, delay, maxWait, callback);
	}

	@Override
	public void signal() {
		lock.lock();
		try {
			cancel(delayFuture);
			delayFuture = scheduler.schedule(this::fire, delay.toNanos(), TimeUnit.NANOSECONDS);
			// scheduled from the first signal of the window, not refreshed by later ones
			if (maxWait != null && maxWaitFuture == null) {
				maxWaitFuture = scheduler.schedule(this::fire, maxWait.toNanos(),
					TimeUnit.NANOSECONDS);
			}

			pending = true;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean flush() {
		return claim() && run();
	}

	@Override
	public void cancel() {
		lock.lock();
		try {
			clear();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isPending() {
		lock.lock();
		try {
			return pending;
		} finally {
			lock.unlock();
		}
	}

	private void fire() {
		if (claim()) {
			run();
		}
	}

	/**
	 * @return {@literal true} if this call took ownership of the pending signals; whoever gets it
	 * is the only one that runs the callback, so a {@link #flush()} racing a scheduled fire cannot
	 * run it twice
	 */
	private boolean claim() {
		lock.lock();
		try {
			if (!pending) {
				return false;
			}

			clear();

			return true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Runs outside the lock, so a callback that calls {@link #signal()} starts a new window
	 * instead of deadlocking or recursing.
	 */
	private boolean run() {
		callback.run();

		return true;
	}

	private void clear() {
		pending = false;
		cancel(delayFuture);
		delayFuture = null;
		cancel(maxWaitFuture);
		maxWaitFuture = null;
	}

	private static void cancel(final @Nullable ScheduledFuture<?> future) {
		if (future != null) {
			future.cancel(false);
		}
	}

}
