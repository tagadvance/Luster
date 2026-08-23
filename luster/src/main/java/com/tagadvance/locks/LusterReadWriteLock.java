package com.tagadvance.locks;

import com.tagadvance.exception.ThrowingSupplier;
import com.tagadvance.exception.ThrowingRunnable;
import com.tagadvance.exception.UncheckedException;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface LusterReadWriteLock extends ReadWriteLock {

	static LusterReadWriteLock newLock() {
		return newLock(ReentrantReadWriteLock::new);
	}

	static LusterReadWriteLock newLock(final Supplier<? extends ReadWriteLock> supplier) {
		final ReadWriteLock lock = supplier.get();

		return wrap(lock);
	}

	static LusterReadWriteLock wrap(final ReadWriteLock lock) {
		return () -> lock;
	}

	ReadWriteLock lock();

	@Override
	@NonNull
	default Lock readLock() {
		return lock().readLock();
	}

	@Override
	@NonNull
	default Lock writeLock() {
		return lock().writeLock();
	}

	default <E extends Exception> void readLock(final ThrowingRunnable<E> runnable) throws E {
		readLock().lock();
		try {
			runnable.runChecked();
		} finally {
			readLock().unlock();
		}
	}

	default <E extends Exception> void writeLock(final ThrowingRunnable<E> runnable) throws E {
		writeLock().lock();
		try {
			runnable.runChecked();
		} finally {
			writeLock().unlock();
		}
	}

	default <V, E extends Exception> V readLock(final ThrowingSupplier<V, E> supplier) throws E {
		readLock().lock();
		try {
			return supplier.get();
		} finally {
			readLock().unlock();
		}
	}

	default <V, E extends Exception> V writeLock(final ThrowingSupplier<V, E> supplier) throws E {
		writeLock().lock();
		try {
			return supplier.get();
		} finally {
			writeLock().unlock();
		}
	}

	default <V> V readLockUnchecked(final Callable<V> callable) throws UncheckedException {
		readLock().lock();
		try {
			return callable.call();
		} catch (final Exception e) {
			if (e instanceof RuntimeException re) {
				throw re;
			}

			throw new UncheckedException(e);
		} finally {
			readLock().unlock();
		}
	}

	default <V> V writeLockUnchecked(final Callable<V> callable)
		throws UncheckedException {
		writeLock().lock();
		try {
			return callable.call();
		} catch (final Exception e) {
			if (e instanceof RuntimeException re) {
				throw re;
			}

			throw new UncheckedException(e);
		} finally {
			writeLock().unlock();
		}
	}

}
