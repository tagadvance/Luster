package com.tagadvance.logging;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A {@link DebounceLogFactory} builder.
 */
public final class DebounceLogFactoryBuilder {

	// the default limit was chosen somewhat arbitrarily
	private static final int DEFAULT_MAX_LOGS = 1_000;

	private ScheduledExecutorService service;

	private Duration debounceDelay;

	private int maxLogs;

	private LogFlusher flusher;

	public DebounceLogFactoryBuilder() {
		this.debounceDelay = Duration.ofSeconds(6);
		this.maxLogs = DEFAULT_MAX_LOGS;
	}

	/**
	 * @param service a {@link ScheduledExecutorService}
	 * @return {@link DebounceLogFactoryBuilder this}
	 */
	public DebounceLogFactoryBuilder withScheduledExecutorService(
		final ScheduledExecutorService service) {
		this.service = requireNonNull(service);

		return this;
	}

	/**
	 * @param delay the {@link Duration debounce delay}
	 * @return {@link DebounceLogFactoryBuilder this}
	 */
	public DebounceLogFactoryBuilder withDebounceDelay(final Duration delay) {
		requireNonNull(delay, "delay must not be null");
		this.debounceDelay = delay;

		return this;
	}

	/**
	 * @param maxLogs the maximum number of logs to store in the queue
	 * @return {@link DebounceLogFactoryBuilder this}
	 */
	public DebounceLogFactoryBuilder withMaxLogs(final int maxLogs) {
		checkArgument(maxLogs > 0, "maxLogs must be > 0");
		this.maxLogs = maxLogs;

		return this;
	}

	/**
	 * @param flusher a {@link LogFlusher flusher}
	 * @return {@link DebounceLogFactoryBuilder this}
	 */
	public DebounceLogFactoryBuilder withLogFlusher(final LogFlusher flusher) {
		this.flusher = requireNonNull(flusher, "flusher must not be null");

		return this;
	}

	/**
	 * Construct a {@link DebounceLogFactory}.
	 *
	 * @return a {@link DebounceLogFactory}
	 */
	public DebounceLogFactory build() {
		return new DebounceLogFactory(
			service == null ? Executors.newSingleThreadScheduledExecutor() : service, debounceDelay,
			maxLogs, flusher);
	}

}
