package com.tagadvance.utilities;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A utility that ensures an operation is processed only once.
 */
public final class Once {

	public static <T> Supplier<T> supplier(final Supplier<T> supplier) {
		final var ref = new AtomicReference<T>();

		return () -> {
			var result = ref.get();
			if (result == null) {
				result = supplier.get();
				if (!ref.compareAndSet(null, result)) {
					return ref.get();
				}
			}

			return result;
		};
	}

	public static Runnable runnable(final Runnable runnable) {
		final var isInit = new AtomicBoolean();

		return () -> {
			if (isInit.compareAndSet(false, true)) {
				runnable.run();
			}
		};
	}

	public static <V> Callable<V> callable(final Callable<V> callable) {
		final var ref = new AtomicReference<V>();

		return () -> {
			var result = ref.get();
			if (result == null) {
				result = callable.call();
				if (!ref.compareAndSet(null, result)) {
					return ref.get();
				}
			}

			return result;
		};
	}

	private Once() {

	}

}
