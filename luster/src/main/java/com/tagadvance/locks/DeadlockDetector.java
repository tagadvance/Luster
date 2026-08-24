package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Finds deadlocks among the {@link ScopedLock locks} of a {@link LockRegistry}.
 * <p>
 * This covers the case {@link java.lang.management.ThreadMXBean#findDeadlockedThreads()} cannot
 * see — a cycle through locks held in shared mode, which record no owner for the JVM to follow.
 * Prefer {@code ThreadMXBean} for everything else; it needs no instrumentation and sees
 * {@code synchronized} monitors too.
 *
 * <pre>{@code
 * final var registry = LockRegistry.create();
 * final var lock = Locks.instrumented(registry);
 *
 * final var detector = DeadlockDetector.of(registry);
 * detector.detect().forEach(deadlock -> ...);
 * }</pre>
 */
public final class DeadlockDetector {

	private final LockRegistry registry;

	private final DeadlockHandler handler;

	/**
	 * @param registry the registry to inspect
	 * @return a detector reporting through {@link DeadlockHandler#logging()}
	 */
	public static DeadlockDetector of(final LockRegistry registry) {
		return of(registry, DeadlockHandler.logging());
	}

	/**
	 * @param registry the registry to inspect
	 * @param handler  what to do with each cycle found by {@link #poll}
	 * @return a detector
	 */
	public static DeadlockDetector of(final LockRegistry registry, final DeadlockHandler handler) {
		return new DeadlockDetector(registry, handler);
	}

	private DeadlockDetector(final LockRegistry registry, final DeadlockHandler handler) {
		this.registry = requireNonNull(registry, "registry must not be null");
		this.handler = requireNonNull(handler, "handler must not be null");
	}

	/**
	 * Looks for cycles now, on the calling thread. Does not invoke the handler — the caller has
	 * the result.
	 *
	 * @return every cycle currently present, empty if there are none
	 */
	public List<Deadlock> detect() {
		return registry.cycles().stream().map(Deadlock::new).toList();
	}

	/**
	 * Looks for cycles on a schedule, reporting each one to the handler.
	 * <p>
	 * A cycle is permanent, so it is reported on every pass until the process is fixed or dies.
	 * Handlers that log should expect repeats.
	 *
	 * @param scheduler where the check runs
	 * @param interval  how often to check
	 * @return the scheduled check, to cancel when it is no longer wanted
	 */
	public ScheduledFuture<?> poll(final ScheduledExecutorService scheduler,
		final Duration interval) {
		requireNonNull(scheduler, "scheduler must not be null");
		requireNonNull(interval, "interval must not be null");
		if (interval.isNegative() || interval.isZero()) {
			throw new IllegalArgumentException("interval must be positive: %s".formatted(interval));
		}

		final var nanos = interval.toNanos();

		return scheduler.scheduleWithFixedDelay(() -> detect().forEach(handler::onDeadlock), nanos,
			nanos, TimeUnit.NANOSECONDS);
	}

}
