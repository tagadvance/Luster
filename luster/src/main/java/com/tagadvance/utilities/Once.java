package com.tagadvance.utilities;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A utility that ensures an operation is processed only once.
 */
public final class Once {

	/**
	 * Wrap the supplied {@link Supplier} so that it is invoked at most once, no matter how many
	 * threads call the returned {@link Supplier}. The value it returns, {@code null} included, is
	 * cached and returned to every subsequent caller.
	 *
	 * <p>The supplier runs under a {@link java.util.concurrent.locks.ReentrantLock} rather than a
	 * {@literal synchronized} block, so a virtual thread that blocks inside it does not pin its
	 * carrier. That rules out Guava's {@code Suppliers.memoize}, which is otherwise correct but
	 * invokes the supplier inside a monitor.</p>
	 *
	 * @param supplier the {@link Supplier} to invoke once
	 * @param <T>      the type of the supplied value
	 * @return the memoizing {@link Supplier}
	 */
	public static <T> Supplier<T> supplier(final Supplier<T> supplier) {
		requireNonNull(supplier, "supplier must not be null");

		// a ReentrantLock rather than Guava's memoize, which runs the supplier inside a
		// synchronized block; AtomicReference#updateAndGet would re-invoke on CAS failure
		final var lock = new ReentrantLock();
		final var reference = new AtomicReference<Optional<T>>();

		return () -> {
			final var cached = reference.get();
			if (cached != null) {
				return cached.orElse(null);
			}

			lock.lock();
			try {
				final var current = reference.get();
				if (current != null) {
					return current.orElse(null);
				}

				final var value = supplier.get();
				reference.set(Optional.ofNullable(value));

				return value;
			} finally {
				lock.unlock();
			}
		};
	}

	/**
	 * Wrap the supplied {@link Runnable} so that it is invoked at most once, no matter how many
	 * threads call the returned {@link Runnable}.
	 *
	 * @param runnable the {@link Runnable} to invoke once
	 * @return the {@link Runnable} wrapper
	 */
	public static Runnable runnable(final Runnable runnable) {
		requireNonNull(runnable, "runnable must not be null");

		final var isFirstRun = new AtomicBoolean();

		return () -> {
			if (isFirstRun.compareAndSet(false, true)) {
				runnable.run();
			}
		};
	}

	/**
	 * Wrap the supplied {@link Callable} so that it completes successfully at most once, no matter
	 * how many threads call the returned {@link Callable}. The value it returns, {@code null}
	 * included, is cached and returned to every subsequent caller.
	 *
	 * <p>A failure is <em>not</em> cached: the exception propagates to the caller that provoked it
	 * and the next caller invokes the {@link Callable} again. Caching the exception instead would
	 * hand later callers a stack trace belonging to another thread, and would make a transient
	 * failure permanent.</p>
	 *
	 * @param callable the {@link Callable} to invoke once
	 * @param <V>      the type of the returned value
	 * @return the memoizing {@link Callable}
	 */
	public static <V> Callable<V> callable(final Callable<V> callable) {
		requireNonNull(callable, "callable must not be null");

		final var lock = new ReentrantLock();
		final var reference = new AtomicReference<Optional<V>>();

		return () -> {
			final var cached = reference.get();
			if (cached != null) {
				return cached.orElse(null);
			}

			lock.lock();
			try {
				final var current = reference.get();
				if (current != null) {
					return current.orElse(null);
				}

				final var value = callable.call();
				reference.set(Optional.ofNullable(value));

				return value;
			} finally {
				lock.unlock();
			}
		};
	}

	private Once() {

	}

}
