package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A {@link ScopedLock} that reports its holders and waiters to a {@link LockRegistry}.
 * <p>
 * Every method on {@link ScopedLock} routes through {@link #readLock()} or {@link #writeLock()},
 * so wrapping the returned {@link Lock} instruments the whole interface without overriding any of
 * it.
 */
final class InstrumentedScopedLock implements ScopedLock {

	private final ReadWriteLock delegate;

	private final LockRegistry registry;

	private final Lock read;

	private final Lock write;

	/**
	 * Hold counts rather than a plain set, because both locks are reentrant and a release must not
	 * retract a hold the thread still has.
	 */
	private final ConcurrentMap<Thread, Integer> readers = new ConcurrentHashMap<>();

	private final ConcurrentMap<Thread, Integer> writers = new ConcurrentHashMap<>();

	InstrumentedScopedLock(final ReadWriteLock delegate, final LockRegistry registry) {
		this.delegate = requireNonNull(delegate, "delegate must not be null");
		this.registry = requireNonNull(registry, "registry must not be null");
		this.read = new TrackedLock(delegate.readLock(), false);
		this.write = new TrackedLock(delegate.writeLock(), true);
	}

	@Override
	public ReadWriteLock lock() {
		return delegate;
	}

	@Override
	public Lock readLock() {
		return read;
	}

	@Override
	public Lock writeLock() {
		return write;
	}

	Set<Thread> readers() {
		return Set.copyOf(readers.keySet());
	}

	Set<Thread> writers() {
		return Set.copyOf(writers.keySet());
	}

	private final class TrackedLock implements Lock {

		private final Lock delegate;

		private final boolean exclusive;

		private TrackedLock(final Lock delegate, final boolean exclusive) {
			this.delegate = delegate;
			this.exclusive = exclusive;
		}

		@Override
		public void lock() {
			final var thread = Thread.currentThread();
			registry.waiting(thread, InstrumentedScopedLock.this, exclusive);
			try {
				delegate.lock();
			} finally {
				registry.doneWaiting(thread);
			}

			held(thread);
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			final var thread = Thread.currentThread();
			registry.waiting(thread, InstrumentedScopedLock.this, exclusive);
			try {
				delegate.lockInterruptibly();
			} finally {
				registry.doneWaiting(thread);
			}

			held(thread);
		}

		@Override
		public boolean tryLock() {
			final var acquired = delegate.tryLock();
			if (acquired) {
				held(Thread.currentThread());
			}

			return acquired;
		}

		@Override
		public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
			final var thread = Thread.currentThread();
			registry.waiting(thread, InstrumentedScopedLock.this, exclusive);
			final boolean acquired;
			try {
				acquired = delegate.tryLock(time, unit);
			} finally {
				registry.doneWaiting(thread);
			}

			if (acquired) {
				held(thread);
			}

			return acquired;
		}

		@Override
		public void unlock() {
			released(Thread.currentThread());
			delegate.unlock();
		}

		@Override
		public Condition newCondition() {
			return delegate.newCondition();
		}

		private void held(final Thread thread) {
			holders().merge(thread, 1, Integer::sum);
		}

		private void released(final Thread thread) {
			holders().computeIfPresent(thread, (key, holds) -> holds == 1 ? null : holds - 1);
		}

		private ConcurrentMap<Thread, Integer> holders() {
			return exclusive ? writers : readers;
		}

	}

}
