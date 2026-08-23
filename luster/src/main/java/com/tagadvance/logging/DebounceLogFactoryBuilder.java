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

	private Duration debounceTimeout;

	private Integer maxLogs;

	private LogReducer reducer;

	private LogFlusher flusher = new DefaultLogFlusher();

	public DebounceLogFactoryBuilder() {

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
	 * The minimum time to wait to process a log entry. Later matching log entries will further push
	 * back the time until the debounced log entries are processed.
	 *
	 * @param delay the {@link Duration debounce delay}
	 * @return {@link DebounceLogFactoryBuilder this}
	 */
	public DebounceLogFactoryBuilder withDebounceDelay(final Duration delay) {
		this.debounceDelay = requireNonNull(delay, "delay must not be null");

		return this;
	}

	/**
	 * The maximum time to wait to process a log entry.
	 *
	 * @param timeout the {@link Duration debounce timeout}
	 * @return {@link DebounceLogFactoryBuilder this}
	 */
	public DebounceLogFactoryBuilder withDebounceTimeout(final Duration timeout) {
		this.debounceTimeout = requireNonNull(timeout, "timeout must not be null");

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
	 * @param reducer a {@link LogReducer reducer}
	 * @return {@link DebounceLogFactoryBuilder this}
	 */
	public DebounceLogFactoryBuilder withLogReducer(final LogReducer reducer) {
		this.reducer = requireNonNull(reducer, "reducer must not be null");

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
		final var service1 =
			this.service == null ? Executors.newSingleThreadScheduledExecutor() : this.service;
		final var delay = this.debounceDelay == null ? Duration.ofSeconds(6) : this.debounceDelay;
		final var timeout = this.debounceTimeout == null ? Duration.ofMinutes(1) : this.debounceTimeout;
		final var maxLogs = this.maxLogs == null ? DEFAULT_MAX_LOGS : this.maxLogs;
		final var flusher = this.flusher == null ? new DefaultLogFlusher() : this.flusher;

		return new DebounceLogFactory(service1, delay, timeout, maxLogs, reducer,
			flusher);
	}

}
