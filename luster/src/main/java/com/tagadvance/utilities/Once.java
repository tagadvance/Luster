package com.tagadvance.utilities;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Suppliers;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
	 * <p>This delegates to {@link Suppliers#memoize(com.google.common.base.Supplier)}, but takes
	 * and returns the {@link java.util.function} type. Guava is an implementation detail of this
	 * library rather than part of its API, so callers should not need it on their own classpath to
	 * memoize a {@link Supplier}.</p>
	 *
	 * @param supplier the {@link Supplier} to invoke once
	 * @param <T>      the type of the supplied value
	 * @return the memoizing {@link Supplier}
	 */
	public static <T> Supplier<T> supplier(final Supplier<T> supplier) {
		requireNonNull(supplier, "supplier must not be null");

		// Guava memoizes under a lock; AtomicReference#updateAndGet would re-invoke on CAS failure
		final var memoized = Suppliers.memoize(supplier::get);

		return memoized::get;
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

		final var lock = new Object();
		final var reference = new AtomicReference<Optional<V>>();

		return () -> {
			synchronized (lock) {
				final var cached = reference.get();
				if (cached != null) {
					return cached.orElse(null);
				}

				final var value = callable.call();
				reference.set(Optional.ofNullable(value));

				return value;
			}
		};
	}

	private Once() {

	}

}
