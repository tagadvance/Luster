package com.tagadvance.utilities;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@link Benchmark} is a utility for performing micro-benchmarking.
 */
public class Benchmark {

	public static void profile(final Runnable runnable,
		final Consumer<Duration> durationConsumer) {
		final var start = System.nanoTime();
		try {
			runnable.run();
		} finally {
			final var stop = System.nanoTime();
			final var elapsed = Duration.ofNanos(stop - start);
			durationConsumer.accept(elapsed);
		}
	}

	public static Runnable wrap(final Runnable runnable,
		final Consumer<Duration> durationConsumer) {
		return () -> profile(runnable, durationConsumer);
	}

	public static <V> V profile(final Callable<V> callable,
		final Consumer<Duration> durationConsumer) throws Exception {
		final var start = System.nanoTime();
		try {
			return callable.call();
		} finally {
			final var stop = System.nanoTime();
			final var elapsed = Duration.ofNanos(stop - start);
			durationConsumer.accept(elapsed);
		}
	}

	public static <V> Callable<V> wrap(final Callable<V> callable,
		final Consumer<Duration> durationConsumer) {
		return () -> profile(callable, durationConsumer);
	}

	public static <T> T profile(final Supplier<T> supplier,
		final Consumer<Duration> durationConsumer) {
		final var start = System.nanoTime();
		try {
			return supplier.get();
		} finally {
			final var stop = System.nanoTime();
			final var elapsed = Duration.ofNanos(stop - start);
			durationConsumer.accept(elapsed);
		}
	}

	public static <T> Supplier<T> wrap(final Supplier<T> supplier,
		final Consumer<Duration> durationConsumer) {
		return () -> profile(supplier, durationConsumer);
	}

	public static <T> Predicate<T> wrap(final Predicate<T> predicate,
		final Consumer<Duration> durationConsumer) {
		return arg -> {
			final var start = System.nanoTime();
			try {
				return predicate.test(arg);
			} finally {
				final var stop = System.nanoTime();
				final var elapsed = Duration.ofNanos(stop - start);
				durationConsumer.accept(elapsed);
			}
		};
	}

	public static <T, R> Function<T, R> wrap(final Function<T, R> function,
		final Consumer<Duration> durationConsumer) {
		return arg -> {
			final var start = System.nanoTime();
			try {
				return function.apply(arg);
			} finally {
				final var stop = System.nanoTime();
				final var elapsed = Duration.ofNanos(stop - start);
				durationConsumer.accept(elapsed);
			}
		};
	}

	public static <T, U, R> BiFunction<T, U, R> wrap(final BiFunction<T, U, R> function,
		final Consumer<Duration> durationConsumer) {
		return (arg1, arg2) -> {
			final var start = System.nanoTime();
			try {
				return function.apply(arg1, arg2);
			} finally {
				final var stop = System.nanoTime();
				final var elapsed = Duration.ofNanos(stop - start);
				durationConsumer.accept(elapsed);
			}
		};
	}

	public static <T> Consumer<T> wrap(final Consumer<T> consumer,
		final Consumer<Duration> durationConsumer) {
		return arg -> {
			final var start = System.nanoTime();
			try {
				consumer.accept(arg);
			} finally {
				final var stop = System.nanoTime();
				final var elapsed = Duration.ofNanos(stop - start);
				durationConsumer.accept(elapsed);
			}
		};
	}

	private Benchmark() {
		// do nothing
	}

}
