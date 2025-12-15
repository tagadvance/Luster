package com.tagadvance.utilities;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A utility that ensures an operation is processed only once.
 */
public final class Once {

	@SuppressWarnings("all")
	public static <T> Supplier<T> supplier(final Supplier<T> supplier) {
		requireNonNull(supplier, "supplier must not be null");

		final var reference = new AtomicReference<Optional<T>>();

		return () -> reference.updateAndGet(
			value -> value == null ? Optional.of(supplier).map(Supplier::get) : value).orElse(null);
	}

	public static Runnable runnable(final Runnable runnable) {
		requireNonNull(runnable, "runnable must not be null");

		final var isFirstRun = new AtomicBoolean();

		return () -> {
			if (isFirstRun.compareAndSet(false, true)) {
				runnable.run();
			}
		};
	}

	@SuppressWarnings("all")
	public static <V> Callable<V> callable(final Callable<V> callable) {
		requireNonNull(callable, "callable must not be null");

		final var atomicValue = new AtomicReference<Optional<V>>();
		final var atomicException = new AtomicReference<Exception>();

		return () -> {
			synchronized (atomicValue) {
				try {
					final var value = atomicValue.get();
					if (value != null) {
						return value.orElse(null);
					}

					final var exception = atomicException.get();
					if (exception != null) {
						throw exception;
					}

					final var callValue = callable.call();
					final var optional = Optional.ofNullable(callValue);
					atomicValue.set(optional);

					return callValue;
				} catch (final Exception e) {
					atomicException.set(e);

					throw e;
				}
			}
		};
	}

	private Once() {

	}

}
