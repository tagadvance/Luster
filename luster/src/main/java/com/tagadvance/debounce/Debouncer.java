package com.tagadvance.debounce;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Absorbs a burst of events into a smaller number of callback invocations.
 * <p>
 * Each {@link Debouncer} is independent; the only thing instances share is whatever
 * {@link ScheduledExecutorService scheduler} you hand them. The debouncer never owns a scheduler
 * and so never needs shutting down.
 * <p>
 * The callback runs on the scheduler's thread, never the calling thread — with one deliberate
 * exception, {@link #flush()}, which runs it on the caller's so that a shutdown drain actually
 * completes before the scheduler goes away.
 * <p>
 * The callback is not told how many events it absorbed. Buffering is the caller's job, which
 * keeps the debouncer a pure signal.
 */
public interface Debouncer {

	/**
	 * Fires once the signals stop: every {@link #signal()} restarts the wait, and the callback
	 * runs {@literal delay} after the last one.
	 *
	 * @param scheduler the scheduler to run the callback on
	 * @param delay     how long the signals must be quiet before firing
	 * @param callback  the callback
	 * @return a new {@link Debouncer}
	 */
	static Debouncer trailing(final ScheduledExecutorService scheduler, final Duration delay,
		final Runnable callback) {
		return new TrailingDebouncer(scheduler, delay, null, callback);
	}

	/**
	 * Fires immediately on the first {@link #signal()}, then at most once per {@literal interval}.
	 * Signals arriving during an interval are coalesced into a single fire at the end of it, which
	 * begins a new interval, so a signal is never silently dropped.
	 *
	 * @param scheduler the scheduler to run the callback on
	 * @param interval  the minimum time between callbacks
	 * @param callback  the callback
	 * @return a new {@link Debouncer}
	 */
	static Debouncer leading(final ScheduledExecutorService scheduler, final Duration interval,
		final Runnable callback) {
		return new LeadingDebouncer(scheduler, interval, callback);
	}

	/**
	 * Reports that an event occurred. Never blocks, and never runs the callback on the calling
	 * thread.
	 */
	void signal();

	/**
	 * Runs the callback now if anything is pending, on the <em>calling</em> thread. A scheduled
	 * fire for the same pending signals will not also run.
	 *
	 * @return {@literal true} if the callback ran
	 */
	boolean flush();

	/**
	 * Discards anything pending without running the callback.
	 */
	void cancel();

	/**
	 * @return {@literal true} if a signal is waiting for the callback to run
	 */
	boolean isPending();

	/**
	 * Bounds how long signals can keep deferring the callback. When {@literal maxWait} elapses the
	 * callback fires regardless of how busy the signals stay, and the window restarts fresh.
	 *
	 * @param maxWait the longest the callback may be deferred
	 * @return a new {@link Debouncer} with the bound applied
	 * @throws UnsupportedOperationException if this debouncer's wait is already bounded, as a
	 *                                       {@link #leading(ScheduledExecutorService, Duration,
	 *                                       Runnable) leading} one's is by its interval
	 */
	default Debouncer withMaxWait(final Duration maxWait) {
		throw new UnsupportedOperationException(
			"maxWait is only meaningful for a trailing debouncer; a leading one is already bounded by its interval");
	}

}
