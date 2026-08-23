package com.tagadvance.locks;

import com.tagadvance.exception.CheckedCallable;
import com.tagadvance.exception.CheckedRunnable;
import com.tagadvance.exception.UncheckedExecutionException;
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

	default <E extends Exception> void readLock(final CheckedRunnable<E> runnable) {
		readLock().lock();
		try {
			runnable.run();
		} finally {
			readLock().unlock();
		}
	}

	default <E extends Exception> void writeLock(final CheckedRunnable<E> runnable) {
		writeLock().lock();
		try {
			runnable.run();
		} finally {
			writeLock().unlock();
		}
	}

	default <V, E extends Exception> V readLock(final CheckedCallable<V, E> callable) throws E {
		readLock().lock();
		try {
			return callable.call();
		} finally {
			readLock().unlock();
		}
	}

	default <V, E extends Exception> V writeLock(final CheckedCallable<V, E> callable) throws E {
		writeLock().lock();
		try {
			return callable.call();
		} finally {
			writeLock().unlock();
		}
	}

	default <V> V readLockUnchecked(final Callable<V> callable) throws UncheckedExecutionException {
		readLock().lock();
		try {
			return callable.call();
		} catch (final Exception e) {
			if (e instanceof RuntimeException re) {
				throw re;
			}

			throw new UncheckedExecutionException(e);
		} finally {
			readLock().unlock();
		}
	}

	default <V> V writeLockUnchecked(final Callable<V> callable)
		throws UncheckedExecutionException {
		writeLock().lock();
		try {
			return callable.call();
		} catch (final Exception e) {
			if (e instanceof RuntimeException re) {
				throw re;
			}

			throw new UncheckedExecutionException(e);
		} finally {
			writeLock().unlock();
		}
	}

}
