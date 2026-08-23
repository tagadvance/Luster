package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import com.tagadvance.exception.ThrowingSupplier;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * A {@link ReadWriteLock} that can be held with try-with-resources or handed a body to run.
 *
 * <pre>{@code
 * try (final var ignored = lock.write()) {
 * 	map.put(key, value);
 * }
 *
 * final var tenants = lock.readLock(this::loadTenants);
 * }</pre>
 * <p>
 * Any {@link ReadWriteLock} can be adapted with {@link Locks#wrap(ReadWriteLock)}, including
 * {@link StampedLock#asReadWriteLock()}. Optimistic reads cannot be expressed through
 * {@link ReadWriteLock}, so a wrapped {@link StampedLock} gives up that capability.
 */
@FunctionalInterface
public interface RWLock extends ReadWriteLock {

	/**
	 * @return the underlying {@link ReadWriteLock}
	 */
	ReadWriteLock lock();

	@Override
	default Lock readLock() {
		return lock().readLock();
	}

	@Override
	default Lock writeLock() {
		return lock().writeLock();
	}

	/**
	 * Acquires the read lock, blocking until it is available.
	 *
	 * @return the held lock, to be released by {@link LockHandle#close()}
	 */
	default LockHandle read() {
		return acquire(readLock());
	}

	/**
	 * Acquires the write lock, blocking until it is available.
	 *
	 * @return the held lock, to be released by {@link LockHandle#close()}
	 */
	default LockHandle write() {
		return acquire(writeLock());
	}

	/**
	 * Acquires the read lock, giving up after {@literal timeout}.
	 *
	 * @param timeout how long to wait
	 * @return the held lock, or {@link Optional#empty()} if the timeout elapsed first
	 * @throws InterruptedException if interrupted while waiting
	 */
	default Optional<LockHandle> tryRead(final Duration timeout) throws InterruptedException {
		return tryAcquire(readLock(), timeout);
	}

	/**
	 * Acquires the write lock, giving up after {@literal timeout}.
	 *
	 * @param timeout how long to wait
	 * @return the held lock, or {@link Optional#empty()} if the timeout elapsed first
	 * @throws InterruptedException if interrupted while waiting
	 */
	default Optional<LockHandle> tryWrite(final Duration timeout) throws InterruptedException {
		return tryAcquire(writeLock(), timeout);
	}

	/**
	 * Runs {@literal supplier} while holding the read lock.
	 *
	 * @param supplier the body to run
	 * @param <V>      the type of the result
	 * @param <E>      the type of exception that may be thrown
	 * @return the result of the body
	 * @throws E if the body threw
	 */
	default <V, E extends Exception> V readLock(final ThrowingSupplier<V, E> supplier) throws E {
		return withLock(readLock(), supplier);
	}

	/**
	 * Runs {@literal supplier} while holding the write lock.
	 *
	 * @param supplier the body to run
	 * @param <V>      the type of the result
	 * @param <E>      the type of exception that may be thrown
	 * @return the result of the body
	 * @throws E if the body threw
	 */
	default <V, E extends Exception> V writeLock(final ThrowingSupplier<V, E> supplier) throws E {
		return withLock(writeLock(), supplier);
	}

	private static LockHandle acquire(final Lock lock) {
		lock.lock();

		return lock::unlock;
	}

	private static Optional<LockHandle> tryAcquire(final Lock lock, final Duration timeout)
		throws InterruptedException {
		requireNonNull(timeout, "timeout must not be null");

		if (!lock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
			return Optional.empty();
		}

		return Optional.of(lock::unlock);
	}

	private static <V, E extends Exception> V withLock(final Lock lock,
		final ThrowingSupplier<V, E> supplier) throws E {
		requireNonNull(supplier, "supplier must not be null");

		lock.lock();
		try {
			return supplier.get();
		} finally {
			lock.unlock();
		}
	}

}
