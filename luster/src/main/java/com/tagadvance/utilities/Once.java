package com.tagadvance.utilities;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A utility that ensures an operation is processed only once.
 */
public final class Once {

	public static <T> Supplier<T> supplier(final Supplier<T> supplier) {
		final var atomicValue = new AtomicReference<T>();
		final var lock = new ReentrantLock();

		return () -> {
			final var value = atomicValue.get();
			if (value != null) {
				return value;
			}

			lock.lock();
			try {
				final var newValue = supplier.get();
				atomicValue.set(newValue);

				return newValue;
			} finally {
				lock.unlock();
			}
		};
	}

	public static Runnable runnable(final Runnable runnable) {
		final var isFirstRun = new AtomicBoolean();

		return () -> {
			if (isFirstRun.compareAndSet(false, true)) {
				runnable.run();
			}
		};
	}

	public static <V> Callable<V> callable(final Callable<V> callable) {
		final var atomicValue = new AtomicReference<V>();
		final var atomicException = new AtomicReference<Exception>();
		final var lock = new ReentrantLock();

		return () -> {
			final var value = atomicValue.get();
			if (value != null) {
				return value;
			}

			final var exception = atomicException.get();
			if (exception != null) {
				throw exception;
			}

			lock.lock();
			try {
				final var callValue = callable.call();
				atomicValue.set(callValue);

				return callValue;
			} catch (final Exception e) {
				atomicException.set(e);

				throw e;
			} finally {
				lock.unlock();
			}
		};
	}

	private Once() {

	}

}
