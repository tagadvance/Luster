package com.tagadvance.exception;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@link DeferredException} allows one to defer all checked exceptions to an
 * {@link ExceptionHandler} for further processing.
 */
public final class DeferredException {

	private final ExceptionHandler handler;

	/**
	 * Constructs a {@link DeferredException} from the supplied {@link ExceptionHandler}.
	 *
	 * @param handler an {@link ExceptionHandler}, e.g. <code>logger::error</code>
	 */
	public DeferredException(final ExceptionHandler handler) {
		this.handler = requireNonNull(handler, "handler must not be null");
	}

	/**
	 * This method wraps the supplied {@link Callable} in a {@link Supplier} that automatically
	 * defers checked exceptions to the {@link ExceptionHandler}.
	 *
	 * @param callable     a {@link Callable}
	 * @param defaultValue a default value {@link Supplier} to use in the event of an exception
	 * @param <V>          the result type of method call
	 * @return the {@link Supplier} wrapper
	 */
	public <V> Supplier<V> callable(final Callable<V> callable, final Supplier<V> defaultValue) {
		requireNonNull(callable, "callable must not be null");
		requireNonNull(defaultValue, "defaultValue must not be null");

		return () -> {
			try {
				return callable.call();
			} catch (final Throwable t) {
				handler.handleException(t);
			}

			return defaultValue.get();
		};
	}

	/**
	 * This method wraps the supplied {@link CheckedRunnable} in a {@link Runnable} that
	 * automatically defers checked exceptions to the {@link ExceptionHandler}.
	 *
	 * @param runnable a {@link CheckedRunnable}
	 * @param <T>      the type of exception that may be thrown
	 * @return the {@link Runnable} wrapper
	 */
	public <T extends Throwable> Runnable runnable(final CheckedRunnable<T> runnable) {
		requireNonNull(runnable, "runnable must not be null");

		return () -> {
			try {
				runnable.runChecked();
			} catch (final Throwable t) {
				handler.handleException(t);
			}
		};
	}

	/**
	 * This method wraps the supplied {@link CheckedConsumer} in a {@link Consumer} that
	 * automatically defers checked exceptions to the {@link ExceptionHandler}.
	 *
	 * @param consumer a {@link CheckedConsumer}
	 * @param <I>      the type of the input to the operation
	 * @param <T>      the type of exception that may be thrown
	 * @return a {@link Consumer} wrapper
	 */
	public <I, T extends Throwable> Consumer<I> consumer(final CheckedConsumer<I, T> consumer) {
		requireNonNull(consumer, "consumer must not be null");

		return v -> {
			try {
				consumer.acceptChecked(v);
			} catch (final Throwable t) {
				handler.handleException(t);
			}
		};
	}

	/**
	 * This method is an alias of {@link #function(CheckedFunction, Supplier)} with a
	 * {@literal defaultValue} {@link Supplier} that always returns {@literal null}.
	 *
	 * @param function a {@link CheckedFunction}
	 * @param <I>      the type of the input to the function
	 * @param <R>      the type of the result of the function
	 * @param <T>      the type of exception that may be thrown
	 * @return a {@link Function} wrapper
	 */
	public <I, R, T extends Throwable> Function<I, R> function(
		final CheckedFunction<I, R, T> function) {
		return function(function, () -> null);
	}

	/**
	 * This method wraps the supplied {@link CheckedFunction} in a {@link Function} that
	 * automatically defers checked exceptions to the {@link ExceptionHandler}.
	 *
	 * @param function     a {@link CheckedFunction}
	 * @param defaultValue a default value {@link Supplier} to use in the event of an exception
	 * @param <I>          the type of the input to the function
	 * @param <R>          the type of the result of the function
	 * @param <T>          the type of exception that may be thrown
	 * @return a {@link Function} wrapper
	 */
	public <I, R, T extends Throwable> Function<I, R> function(
		final CheckedFunction<I, R, T> function, final Supplier<R> defaultValue) {
		requireNonNull(function, "function must not be null");
		requireNonNull(defaultValue, "defaultValue must not be null");

		return i -> {
			try {
				return function.applyChecked(i);
			} catch (final Throwable t) {
				handler.handleException(t);
			}

			return defaultValue.get();
		};
	}

	/**
	 * This method is an alias of {@link #predicate(CheckedPredicate, Supplier)} with a
	 * {@literal defaultValue} {@link Supplier} that always returns {@link Boolean#FALSE}.
	 *
	 * @param predicate a {@literal CheckedPredicate}
	 * @param <T>       the type of the input to the predicate
	 * @param <E>       the type of exception that may be thrown
	 * @return the {@link Predicate} wrapper
	 */
	public <T, E extends Exception> Predicate<T> predicate(final CheckedPredicate<T, E> predicate) {
		return predicate(predicate, Boolean.FALSE::booleanValue);
	}

	/**
	 * This method wraps the supplied {@link CheckedPredicate} in a {@link Predicate} that
	 * automatically defers checked exceptions to the {@link ExceptionHandler}.
	 *
	 * @param predicate    a {@literal CheckedPredicate}
	 * @param defaultValue a default value {@link Supplier} to use in the event of an exception
	 * @param <T>          the type of the input to the predicate
	 * @param <E>          the type of exception that may be thrown
	 * @return the {@link Predicate} wrapper
	 */
	public <T, E extends Exception> Predicate<T> predicate(final CheckedPredicate<T, E> predicate,
		final Supplier<Boolean> defaultValue) {
		requireNonNull(predicate, "predicate must not be null");
		requireNonNull(defaultValue, "defaultValue must not be null");

		return v -> {
			try {
				return predicate.testChecked(v);
			} catch (final Throwable t) {
				handler.handleException(t);
			}

			return defaultValue.get();
		};
	}

	/**
	 * A handler of exceptions.
	 */
	@FunctionalInterface
	public interface ExceptionHandler {

		/**
		 * This method is invoked when an exception is caught by a deferred wrapper.
		 *
		 * @param t the exception
		 */
		void handleException(Throwable t);

	}

}
