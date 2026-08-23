package com.tagadvance.utilities;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import com.tagadvance.exception.ThrowingSupplier;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@link Timed} is timing instrumentation: it measures how long an operation took and hands the
 * elapsed {@link Duration} to a callback. It is <em>not</em> a benchmarking harness - there is no
 * warmup, no JIT control, no statistics and no defence against dead code elimination, so a number
 * it reports for a small operation says very little about that operation's cost in isolation. Use
 * <a href="https://openjdk.org/projects/code-tools/jmh/">JMH</a> for micro-benchmarks.
 *
 * <p>The elapsed duration is reported even when the operation throws, which is the part most often
 * omitted when a timing probe is pasted in by hand.</p>
 */
public final class Timed {

	/**
	 * Invoke the supplied operation and report how long it took, whether it returns or throws.
	 *
	 * @param supplier         the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @param <V>              the type of the returned value
	 * @param <E>              the type of exception that may be thrown
	 * @return the value returned by the operation
	 * @throws E the type of exception that may be thrown
	 */
	public static <V, E extends Exception> V profile(final ThrowingSupplier<V, E> supplier,
		final Consumer<Duration> durationConsumer) throws E {
		requireNonNull(supplier, "supplier must not be null");
		requireNonNull(durationConsumer, "durationConsumer must not be null");

		final var stopwatch = Stopwatch.createStarted();
		try {
			return supplier.get();
		} finally {
			durationConsumer.accept(stopwatch.elapsed());
		}
	}

	/**
	 * @param runnable         the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @return a {@link Runnable} that times each invocation of the supplied {@link Runnable}
	 */
	public static Runnable wrap(final Runnable runnable,
		final Consumer<Duration> durationConsumer) {
		requireNonNull(runnable, "runnable must not be null");

		return () -> profile(() -> {
			runnable.run();

			return null;
		}, durationConsumer);
	}

	/**
	 * @param callable         the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @param <V>              the type of the returned value
	 * @return a {@link Callable} that times each invocation of the supplied {@link Callable}
	 */
	public static <V> Callable<V> wrap(final Callable<V> callable,
		final Consumer<Duration> durationConsumer) {
		requireNonNull(callable, "callable must not be null");

		return () -> profile(callable::call, durationConsumer);
	}

	/**
	 * @param supplier         the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @param <T>              the type of the supplied value
	 * @return a {@link Supplier} that times each invocation of the supplied {@link Supplier}
	 */
	public static <T> Supplier<T> wrap(final Supplier<T> supplier,
		final Consumer<Duration> durationConsumer) {
		requireNonNull(supplier, "supplier must not be null");

		return () -> profile(supplier::get, durationConsumer);
	}

	/**
	 * @param predicate        the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @param <T>              the type of the input to the predicate
	 * @return a {@link Predicate} that times each invocation of the supplied {@link Predicate}
	 */
	// an implicitly typed lambda cannot pick between the single-argument overloads; callers
	// disambiguate with an explicit target type, which is cheaper than dropping the adapters
	@SuppressWarnings("overloads")
	public static <T> Predicate<T> wrap(final Predicate<T> predicate,
		final Consumer<Duration> durationConsumer) {
		requireNonNull(predicate, "predicate must not be null");

		return arg -> profile(() -> predicate.test(arg), durationConsumer);
	}

	/**
	 * @param function         the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @param <T>              the type of the input to the function
	 * @param <R>              the type of the result of the function
	 * @return a {@link Function} that times each invocation of the supplied {@link Function}
	 */
	@SuppressWarnings("overloads")
	public static <T, R> Function<T, R> wrap(final Function<T, R> function,
		final Consumer<Duration> durationConsumer) {
		requireNonNull(function, "function must not be null");

		return arg -> profile(() -> function.apply(arg), durationConsumer);
	}

	/**
	 * @param function         the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @param <T>              the type of the first argument to the function
	 * @param <U>              the type of the second argument to the function
	 * @param <R>              the type of the result of the function
	 * @return a {@link BiFunction} that times each invocation of the supplied {@link BiFunction}
	 */
	public static <T, U, R> BiFunction<T, U, R> wrap(final BiFunction<T, U, R> function,
		final Consumer<Duration> durationConsumer) {
		requireNonNull(function, "function must not be null");

		return (arg1, arg2) -> profile(() -> function.apply(arg1, arg2), durationConsumer);
	}

	/**
	 * @param consumer         the operation to time
	 * @param durationConsumer a callback that receives the elapsed {@link Duration}
	 * @param <T>              the type of the input to the consumer
	 * @return a {@link Consumer} that times each invocation of the supplied {@link Consumer}
	 */
	@SuppressWarnings("overloads")
	public static <T> Consumer<T> wrap(final Consumer<T> consumer,
		final Consumer<Duration> durationConsumer) {
		requireNonNull(consumer, "consumer must not be null");

		return arg -> profile(() -> {
			consumer.accept(arg);

			return null;
		}, durationConsumer);
	}

	private Timed() {

	}

}
