package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Creates {@link ScopedLock locks}.
 */
public final class Locks {

	/**
	 * @return a new {@link ScopedLock} backed by a {@link ReentrantReadWriteLock}
	 */
	public static ScopedLock newLock() {
		return wrap(new ReentrantReadWriteLock());
	}

	/**
	 * @param lock the {@link ReadWriteLock} to adapt
	 * @return an {@link ScopedLock} delegating to {@literal lock}
	 */
	public static ScopedLock wrap(final ReadWriteLock lock) {
		requireNonNull(lock, "lock must not be null");

		return () -> lock;
	}

	/**
	 * Creates a lock that reports its holders and waiters to {@literal registry}, so that
	 * {@link DeadlockDetector} can find cycles the JVM cannot.
	 * <p>
	 * This costs a map update on every acquisition and release — the bookkeeping
	 * {@link java.util.concurrent.locks.AbstractQueuedSynchronizer} deliberately omits for
	 * performance. {@link #newLock()} is untouched by it and stays free; instrument only the locks
	 * you actually want to watch.
	 *
	 * @param registry the registry to report to
	 * @return an instrumented {@link ScopedLock} backed by a {@link ReentrantReadWriteLock}
	 */
	public static ScopedLock instrumented(final LockRegistry registry) {
		return instrumented(new ReentrantReadWriteLock(), registry);
	}

	/**
	 * @param lock     the {@link ReadWriteLock} to adapt
	 * @param registry the registry to report to
	 * @return an instrumented {@link ScopedLock} delegating to {@literal lock}
	 * @see #instrumented(LockRegistry)
	 */
	public static ScopedLock instrumented(final ReadWriteLock lock, final LockRegistry registry) {
		requireNonNull(lock, "lock must not be null");
		requireNonNull(registry, "registry must not be null");

		return new InstrumentedScopedLock(lock, registry);
	}

	private Locks() {
	}

}
