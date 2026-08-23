package com.tagadvance.exception;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Hands failures to a handler and keeps going, so one bad element does not abort a pipeline.
 * <pre>{@code
 * final var onError = OnError.of(logger::error);
 * stream.map(onError.optionalFunction(this::parse))
 * 	.flatMap(Optional::stream)
 * 	.forEach(System.out::println);
 * }</pre>
 * <p>
 * Unlike the {@code Checked*} adapters, the wrappers here catch unchecked exceptions too. That is
 * deliberate: the adapters only erase checked-ness, whereas handle-and-continue is this type's
 * entire purpose.
 *
 * @see ExceptionCollector for the collect-now, throw-once-at-the-end variant
 */
@FunctionalInterface
public interface OnError {

	/**
	 * Gives a handler lambda a target type, e.g. {@code OnError.of(logger::error)}.
	 *
	 * @param handler an exception handler
	 * @return the handler
	 */
	static OnError of(final OnError handler) {
		return requireNonNull(handler, "handler must not be null");
	}

	/**
	 * This method is invoked when an exception is caught by one of the wrappers below.
	 *
	 * @param e the exception
	 */
	void handleException(Exception e);

	/**
	 * @param runnable a {@link ThrowingRunnable}
	 * @param <E>      the type of exception that may be thrown
	 * @return a {@link Runnable} that defers failures to this handler
	 */
	default <E extends Exception> Runnable runnable(final ThrowingRunnable<E> runnable) {
		requireNonNull(runnable, "runnable must not be null");

		return () -> {
			try {
				runnable.runChecked();
			} catch (final Exception e) {
				handleException(e);
			}
		};
	}

	/**
	 * @param consumer a {@link ThrowingConsumer}
	 * @param <I>      the type of the input to the operation
	 * @param <E>      the type of exception that may be thrown
	 * @return a {@link Consumer} that defers failures to this handler
	 */
	default <I, E extends Exception> Consumer<I> consumer(final ThrowingConsumer<I, E> consumer) {
		requireNonNull(consumer, "consumer must not be null");

		return i -> {
			try {
				consumer.acceptChecked(i);
			} catch (final Exception e) {
				handleException(e);
			}
		};
	}

	/**
	 * Alias of {@link #supplier(ThrowingSupplier, Supplier)} where the default value is
	 * {@literal null}. Prefer {@link #optionalSupplier(ThrowingSupplier)}, which does not conflate
	 * failure with a legitimate {@literal null}.
	 *
	 * @param supplier a {@link ThrowingSupplier}
	 * @param <V>      the result type
	 * @param <E>      the type of exception that may be thrown
	 * @return a {@link Supplier} that defers failures to this handler
	 */
	default <V, E extends Exception> Supplier<V> supplier(final ThrowingSupplier<V, E> supplier) {
		return supplier(supplier, () -> null);
	}

	/**
	 * @param supplier     a {@link ThrowingSupplier}
	 * @param defaultValue a default value {@link Supplier} to use in the event of an exception
	 * @param <V>          the result type
	 * @param <E>          the type of exception that may be thrown
	 * @return a {@link Supplier} that defers failures to this handler
	 */
	default <V, E extends Exception> Supplier<V> supplier(final ThrowingSupplier<V, E> supplier,
		final Supplier<V> defaultValue) {
		requireNonNull(supplier, "supplier must not be null");
		requireNonNull(defaultValue, "defaultValue must not be null");

		return () -> {
			try {
				return supplier.get();
			} catch (final Exception e) {
				handleException(e);
			}

			return defaultValue.get();
		};
	}

	/**
	 * @param supplier a {@link ThrowingSupplier}
	 * @param <V>      the result type
	 * @param <E>      the type of exception that may be thrown
	 * @return a {@link Supplier} yielding {@link Optional#empty()} on failure
	 */
	default <V, E extends Exception> Supplier<Optional<V>> optionalSupplier(
		final ThrowingSupplier<V, E> supplier) {
		requireNonNull(supplier, "supplier must not be null");

		return () -> {
			try {
				return Optional.ofNullable(supplier.get());
			} catch (final Exception e) {
				handleException(e);
			}

			return Optional.empty();
		};
	}

	/**
	 * Alias of {@link #function(ThrowingFunction, Supplier)} where the default value is
	 * {@literal null}. Prefer {@link #optionalFunction(ThrowingFunction)}, which does not conflate
	 * failure with a legitimate {@literal null}.
	 *
	 * @param function a {@link ThrowingFunction}
	 * @param <I>      the type of the input to the function
	 * @param <R>      the type of the result of the function
	 * @param <E>      the type of exception that may be thrown
	 * @return a {@link Function} that defers failures to this handler
	 */
	default <I, R, E extends Exception> Function<I, R> function(
		final ThrowingFunction<I, R, E> function) {
		return function(function, () -> null);
	}

	/**
	 * @param function     a {@link ThrowingFunction}
	 * @param defaultValue a default value {@link Supplier} to use in the event of an exception
	 * @param <I>          the type of the input to the function
	 * @param <R>          the type of the result of the function
	 * @param <E>          the type of exception that may be thrown
	 * @return a {@link Function} that defers failures to this handler
	 */
	default <I, R, E extends Exception> Function<I, R> function(
		final ThrowingFunction<I, R, E> function, final Supplier<R> defaultValue) {
		requireNonNull(function, "function must not be null");
		requireNonNull(defaultValue, "defaultValue must not be null");

		return i -> {
			try {
				return function.applyChecked(i);
			} catch (final Exception e) {
				handleException(e);
			}

			return defaultValue.get();
		};
	}

	/**
	 * @param function a {@link ThrowingFunction}
	 * @param <I>      the type of the input to the function
	 * @param <R>      the type of the result of the function
	 * @param <E>      the type of exception that may be thrown
	 * @return a {@link Function} yielding {@link Optional#empty()} on failure
	 */
	default <I, R, E extends Exception> Function<I, Optional<R>> optionalFunction(
		final ThrowingFunction<I, R, E> function) {
		requireNonNull(function, "function must not be null");

		return i -> {
			try {
				return Optional.ofNullable(function.applyChecked(i));
			} catch (final Exception e) {
				handleException(e);
			}

			return Optional.empty();
		};
	}

	/**
	 * Alias of {@link #predicate(ThrowingPredicate, BooleanSupplier)} where the default value is
	 * {@literal false}.
	 *
	 * @param predicate a {@link ThrowingPredicate}
	 * @param <I>       the type of the input to the predicate
	 * @param <E>       the type of exception that may be thrown
	 * @return a {@link Predicate} that defers failures to this handler
	 */
	default <I, E extends Exception> Predicate<I> predicate(
		final ThrowingPredicate<I, E> predicate) {
		return predicate(predicate, () -> false);
	}

	/**
	 * @param predicate    a {@link ThrowingPredicate}
	 * @param defaultValue a default value {@link BooleanSupplier} to use in the event of an
	 *                     exception
	 * @param <I>          the type of the input to the predicate
	 * @param <E>          the type of exception that may be thrown
	 * @return a {@link Predicate} that defers failures to this handler
	 */
	default <I, E extends Exception> Predicate<I> predicate(final ThrowingPredicate<I, E> predicate,
		final BooleanSupplier defaultValue) {
		requireNonNull(predicate, "predicate must not be null");
		requireNonNull(defaultValue, "defaultValue must not be null");

		return i -> {
			try {
				return predicate.testChecked(i);
			} catch (final Exception e) {
				handleException(e);
			}

			return defaultValue.getAsBoolean();
		};
	}

}
