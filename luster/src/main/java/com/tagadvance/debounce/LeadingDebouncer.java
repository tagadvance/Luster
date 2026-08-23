package com.tagadvance.debounce;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

final class LeadingDebouncer implements Debouncer {

	private final ScheduledExecutorService scheduler;

	private final Duration interval;

	private final Runnable callback;

	private final Lock lock = new ReentrantLock();

	private @Nullable ScheduledFuture<?> intervalFuture;

	/**
	 * {@literal true} while an interval is open, i.e. a callback has run recently enough that
	 * another must wait.
	 */
	private boolean throttling;

	private boolean pending;

	LeadingDebouncer(final ScheduledExecutorService scheduler, final Duration interval,
		final Runnable callback) {
		this.scheduler = requireNonNull(scheduler, "scheduler must not be null");
		requireNonNull(interval, "interval must not be null");
		if (interval.isNegative() || interval.isZero()) {
			throw new IllegalArgumentException("interval must be positive: %s".formatted(interval));
		}

		this.interval = interval;
		this.callback = requireNonNull(callback, "callback must not be null");
	}

	@Override
	public void signal() {
		final boolean fireNow;
		lock.lock();
		try {
			fireNow = !throttling;
			if (fireNow) {
				throttling = true;
				openInterval();
			} else {
				pending = true;
			}
		} finally {
			lock.unlock();
		}

		if (fireNow) {
			// the contract says the callback never runs on the calling thread
			scheduler.execute(callback);
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
			pending = false;
			throttling = false;
			if (intervalFuture != null) {
				intervalFuture.cancel(false);
				intervalFuture = null;
			}
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

	private void onIntervalEnd() {
		final boolean fire;
		lock.lock();
		try {
			fire = pending;
			pending = false;
			if (fire) {
				// something arrived while throttled; fire once and open a fresh interval
				openInterval();
			} else {
				throttling = false;
				intervalFuture = null;
			}
		} finally {
			lock.unlock();
		}

		if (fire) {
			callback.run();
		}
	}

	private void openInterval() {
		intervalFuture = scheduler.schedule(this::onIntervalEnd, interval.toNanos(),
			TimeUnit.NANOSECONDS);
	}

	private boolean claim() {
		lock.lock();
		try {
			if (!pending) {
				return false;
			}

			pending = false;

			return true;
		} finally {
			lock.unlock();
		}
	}

	private boolean run() {
		callback.run();

		return true;
	}

}
