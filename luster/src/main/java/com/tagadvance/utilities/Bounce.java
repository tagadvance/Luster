package com.tagadvance.utilities;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Bounce {

	private final ScheduledExecutorService service;

	public Bounce() {
		this(Executors.newSingleThreadScheduledExecutor(runnable -> {
			final var thread = new Thread(runnable, "Debounce-Thread");
			thread.setDaemon(true);

			return thread;
		}));
	}

	public Bounce(final ScheduledExecutorService service) {
		this.service = requireNonNull(service, "service must not be null");
	}

	public Debouncer debounce(final Runnable callback, final Duration delay) {
		requireNonNull(callback, "callback must not be null");
		requireNonNull(delay, "delay must not be null");

		final var stopwatch = Stopwatch.createUnstarted();
		final var debounceFuture = new AtomicReference<Future<?>>();

		return new Debouncer() {

			@Override
			public void cancel(final boolean mayInterruptIfRunning) {
				debounceFuture.updateAndGet(future -> {
					if (future != null) {
						stopwatch.reset();
						future.cancel(mayInterruptIfRunning);
					}

					return null;
				});
			}

			@Override
			public void bounce() {
				debounceFuture.updateAndGet(future -> {
					if (future != null) {
						stopwatch.reset();
						future.cancel(false);
					}

					stopwatch.start();

					return service.schedule(() -> {
							if (stopwatch.elapsed().compareTo(delay.plusMillis(100)) > 0) {
								// log a warning that the debounce queue is
							}

							service.submit(callback);
						},
						delay.toNanos(), TimeUnit.NANOSECONDS);
				});
			}

		};
	}

	public Debouncer debounceImmediately(final Runnable callback, final Duration delay) {
		requireNonNull(callback, "callback must not be null");
		requireNonNull(delay, "delay must not be null");

		final var debounceFuture = new AtomicReference<Future<?>>();

		return new Debouncer() {

			@Override
			public void cancel(final boolean mayInterruptIfRunning) {
				debounceFuture.updateAndGet(future -> {
					if (future != null) {
						future.cancel(mayInterruptIfRunning);
					}

					return null;
				});
			}

			@Override
			public void bounce() {
				debounceFuture.updateAndGet(future -> {
					if (future == null) {
						return service.submit(callback);
					}

					return service.schedule(() -> debounceFuture.set(null),
						delay.toNanos(), TimeUnit.NANOSECONDS);
				});
			}

		};
	}

	public interface Debouncer {

		void cancel(boolean mayInterruptIfRunning);

		void bounce();

	}

}
