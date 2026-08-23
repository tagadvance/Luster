package com.tagadvance.debounce;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ScheduledExecutorService} with a virtual clock, so debounce timing can be tested
 * without sleeping. Only the operations {@link Debouncer} actually uses are implemented.
 */
public final class ManualScheduler implements ScheduledExecutorService {

	private static final int MAX_ITERATIONS = 1_000;

	private final List<Task> tasks = new ArrayList<>();

	private long now;

	/**
	 * Advances the clock and runs everything that comes due.
	 */
	public void advance(final Duration duration) {
		now += duration.toNanos();
		runDueTasks();
	}

	/**
	 * Runs everything already due without moving the clock.
	 */
	public void runDueTasks() {
		for (int i = 0; i < MAX_ITERATIONS; i++) {
			final var due = tasks.stream()
				.filter(task -> !task.cancelled && task.time <= now)
				.min(Comparator.comparingLong(task -> task.time))
				.orElse(null);
			if (due == null) {
				return;
			}

			tasks.remove(due);
			due.done = true;
			due.runnable.run();
		}

		throw new IllegalStateException("tasks kept rescheduling themselves");
	}

	public int pendingTaskCount() {
		return (int) tasks.stream().filter(task -> !task.cancelled).count();
	}

	@Override
	public ScheduledFuture<?> schedule(final Runnable command, final long delay,
		final TimeUnit unit) {
		final var task = new Task(now + unit.toNanos(delay), command);
		tasks.add(task);

		return task;
	}

	@Override
	public void execute(final Runnable command) {
		tasks.add(new Task(now, command));
	}

	private final class Task implements ScheduledFuture<Object> {

		private final long time;

		private final Runnable runnable;

		private boolean cancelled;

		private boolean done;

		private Task(final long time, final Runnable runnable) {
			this.time = time;
			this.runnable = runnable;
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			if (done || cancelled) {
				return false;
			}

			cancelled = true;
			tasks.remove(this);

			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return done || cancelled;
		}

		@Override
		public long getDelay(final TimeUnit unit) {
			return unit.convert(time - now, TimeUnit.NANOSECONDS);
		}

		@Override
		public int compareTo(final Delayed o) {
			return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
		}

		@Override
		public Object get() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(final long timeout, final TimeUnit unit) {
			throw new UnsupportedOperationException();
		}

	}

	@Override
	public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
		final TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
		final long period, final TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command,
		final long initialDelay, final long delay, final TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isShutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isTerminated() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean awaitTermination(final long timeout, final TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Future<T> submit(final Callable<T> task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Future<T> submit(final Runnable task, final T result) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<?> submit(final Runnable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
		final long timeout, final TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout,
		final TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

}
