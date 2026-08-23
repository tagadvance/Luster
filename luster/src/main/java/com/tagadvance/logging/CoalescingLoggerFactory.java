package com.tagadvance.logging;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * Wraps another {@link ILoggerFactory} so that a burst of identical log events becomes one full
 * log line plus one summary.
 * <p>
 * The first occurrence of a {@link Fingerprint} is logged <em>synchronously, on the calling
 * thread, unchanged</em> — so its timestamp, MDC, thread name and stack trace are the real ones,
 * and caller data is preserved through {@link org.slf4j.spi.LocationAwareLogger}. Every repeat
 * inside the window is counted and dropped. When the window closes, one summary line is emitted
 * naming how many were suppressed, over what period, with a bounded sample of their arguments.
 * <p>
 * The only fidelity ever lost belongs to events that were going to be discarded anyway.
 *
 * <pre>{@code
 * try (final var factory = CoalescingLoggerFactory.builder()
 * 	.withQuietPeriod(Duration.ofSeconds(6))
 * 	.withMaxWait(Duration.ofMinutes(1))
 * 	.build()) {
 * 	final Logger logger = factory.getLogger(MyJob.class.getName());
 * 	...
 * } // pending summaries are emitted on close
 * }</pre>
 */
public final class CoalescingLoggerFactory implements ILoggerFactory, AutoCloseable {

	private static final String SUMMARY_FORMAT =
		"Suppressed {} further occurrences of \"{}\" over {}, first at {}{}";

	private final ILoggerFactory delegate;

	private final ScheduledExecutorService scheduler;

	private final boolean ownsScheduler;

	private final Duration quietPeriod;

	private final Duration maxWait;

	private final UnaryOperator<String> messageNormalizer;

	private final Clock clock;

	private final @Nullable Marker bypassMarker;

	private final ConcurrentMap<String, CoalescingLogger> loggersByName = new ConcurrentHashMap<>();

	private final ConcurrentMap<Fingerprint, Suppression> suppressions = new ConcurrentHashMap<>();

	private CoalescingLoggerFactory(final Builder builder) {
		this.delegate = builder.delegate == null ? LoggerFactory.getILoggerFactory()
			: builder.delegate;
		this.ownsScheduler = builder.scheduler == null;
		this.scheduler = ownsScheduler ? newDaemonScheduler() : builder.scheduler;
		this.quietPeriod = builder.quietPeriod;
		this.maxWait = builder.maxWait;
		this.messageNormalizer = builder.messageNormalizer;
		this.clock = builder.clock;
		this.bypassMarker = builder.bypassMarker;
	}

	private static ScheduledExecutorService newDaemonScheduler() {
		return Executors.newSingleThreadScheduledExecutor(runnable -> {
			final var thread = new Thread(runnable, "CoalescingLogger");
			thread.setDaemon(true);

			return thread;
		});
	}

	/**
	 * @return a new {@link Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Logger getLogger(final String name) {
		requireNonNull(name, "name must not be null");

		return loggersByName.computeIfAbsent(name,
			key -> new CoalescingLogger(key, delegate.getLogger(key), this));
	}

	/**
	 * Emits a summary for every open window, on the calling thread. Called automatically by
	 * {@link #close()}.
	 */
	public void flush() {
		suppressions.values().forEach(Suppression::flush);
	}

	/**
	 * Emits pending summaries and, if this factory created its own scheduler, shuts it down.
	 */
	@Override
	public void close() {
		flush();
		if (ownsScheduler) {
			scheduler.shutdownNow();
		}
	}

	void log(final CoalescingLogger source, final Level level, final @Nullable Marker marker,
		final @Nullable String messagePattern, final Object @Nullable [] arguments,
		final @Nullable Throwable throwable) {
		final var format = messagePattern == null ? "" : messagePattern;
		if (isBypassed(marker)) {
			source.emit(level, marker, format, arguments, throwable);

			return;
		}

		final var normalized = messageNormalizer.apply(format);
		final var fingerprint = Fingerprint.of(source.getName(), level, normalized, throwable);
		final var suppression = suppressions.computeIfAbsent(fingerprint,
			key -> new Suppression(key, clock.instant(), scheduler, quietPeriod, maxWait,
				this::closeWindow));

		if (suppression.claimFirst(marker, throwable)) {
			source.emit(level, marker, format, arguments, throwable);
		} else {
			suppression.record(arguments);
		}

		suppression.signal();
	}

	private boolean isBypassed(final @Nullable Marker marker) {
		return bypassMarker != null && marker != null && marker.contains(bypassMarker);
	}

	/**
	 * The window is removed before the summary is emitted, so the next event of this fingerprint
	 * is a fresh first occurrence and is logged in full. A repeat racing this removal loses its
	 * count, which is a count of noise.
	 */
	private void closeWindow(final Suppression suppression) {
		final var fingerprint = suppression.fingerprint();
		suppressions.remove(fingerprint, suppression);

		suppression.summary(clock.instant()).ifPresent(args -> {
			final var logger = loggersByName.get(fingerprint.loggerName());
			if (logger != null) {
				logger.emit(fingerprint.level(), suppression.marker(), SUMMARY_FORMAT, args, null);
			}
		});
	}

	/**
	 * Configures a {@link CoalescingLoggerFactory}.
	 */
	public static final class Builder {

		private @Nullable ILoggerFactory delegate;

		private @Nullable ScheduledExecutorService scheduler;

		private Duration quietPeriod = Duration.ofSeconds(6);

		private Duration maxWait = Duration.ofMinutes(1);

		private UnaryOperator<String> messageNormalizer = MessageNormalizer.collapsingValues();

		private Clock clock = Clock.systemUTC();

		private @Nullable Marker bypassMarker;

		private Builder() {
		}

		/**
		 * @param delegate where the real loggers come from; defaults to
		 *                 {@link LoggerFactory#getILoggerFactory()}
		 * @return this builder
		 */
		public Builder withDelegate(final ILoggerFactory delegate) {
			this.delegate = requireNonNull(delegate, "delegate must not be null");

			return this;
		}

		/**
		 * @param scheduler runs the summary emissions; if unset, the factory creates a daemon
		 *                  scheduler and shuts it down on {@link #close()}
		 * @return this builder
		 */
		public Builder withScheduler(final ScheduledExecutorService scheduler) {
			this.scheduler = requireNonNull(scheduler, "scheduler must not be null");

			return this;
		}

		/**
		 * @param quietPeriod how long repeats must stop before the window closes
		 * @return this builder
		 */
		public Builder withQuietPeriod(final Duration quietPeriod) {
			this.quietPeriod = requireNonNull(quietPeriod, "quietPeriod must not be null");

			return this;
		}

		/**
		 * @param maxWait the longest a window may stay open under a continuous storm
		 * @return this builder
		 */
		public Builder withMaxWait(final Duration maxWait) {
			this.maxWait = requireNonNull(maxWait, "maxWait must not be null");

			return this;
		}

		/**
		 * @param messageNormalizer applied to the message before fingerprinting, so that
		 *                          pre-formatted messages coalesce; pass
		 *                          {@link UnaryOperator#identity()} to disable
		 * @return this builder
		 */
		public Builder withMessageNormalizer(final UnaryOperator<String> messageNormalizer) {
			this.messageNormalizer = requireNonNull(messageNormalizer,
				"messageNormalizer must not be null");

			return this;
		}

		/**
		 * Registers a {@link Marker} that opts an event out of coalescing entirely: any event
		 * carrying it is logged every time, never counted and never suppressed. Use it for events
		 * that must not be dropped, such as audit records.
		 *
		 * @param bypassMarker the marker to bypass on
		 * @return this builder
		 */
		public Builder withBypassMarker(final Marker bypassMarker) {
			this.bypassMarker = requireNonNull(bypassMarker, "bypassMarker must not be null");

			return this;
		}

		Builder withClock(final Clock clock) {
			this.clock = requireNonNull(clock, "clock must not be null");

			return this;
		}

		/**
		 * @return a new {@link CoalescingLoggerFactory}
		 */
		public CoalescingLoggerFactory build() {
			return new CoalescingLoggerFactory(this);
		}

	}

}
