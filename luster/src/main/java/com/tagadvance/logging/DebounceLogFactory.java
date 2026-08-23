package com.tagadvance.logging;

import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationInterceptor;
import com.tagadvance.proxy.InvocationProxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class DebounceLogFactory implements ILoggerFactory {

	private final Lock lock = new ReentrantLock();

	final Map<DebounceKey, DebounceLog> logs = new HashMap<>();

	private final ScheduledExecutorService service;

	private final Duration debounceDelay;

	private final Duration debounceTimeout;

	private final int maxLogs;

	private final LogReducer reducer;

	private final LogFlusher flusher;

	DebounceLogFactory(final ScheduledExecutorService service, final Duration debounceDelay,
		final Duration debounceTimeout, final int maxLogs, final LogReducer reducer,
		final LogFlusher flusher) {
		this.service = service;
		this.debounceDelay = debounceDelay;
		this.debounceTimeout = debounceTimeout;
		this.maxLogs = maxLogs;
		this.reducer = reducer;
		this.flusher = flusher;
	}

	@Override
	public Logger getLogger(final String name) {
		final var logger = LoggerFactory.getLogger(name);

		return InvocationProxy.createProxy(Logger.class, logger, new LogInterceptor(logger));
	}

	private int size() {
		return logs.values().stream().mapToInt(DebounceLog::size).sum();
	}

	private class LogInterceptor implements InvocationInterceptor {

		private final Logger logger;

		LogInterceptor(final Logger logger) {
			this.logger = logger;
		}

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			final var logEntry = LogEntry.fromInvocation(invocation);
			if (logEntry.isPresent()) {
				final var e = logEntry.get();
				// ignore log entries that were formatted externally
				if (e.getFormat().contains("{}")) {
					intercept(e);

					return null;
				}
			}

			return invocation.invoke();
		}

		private void intercept(final LogEntry e) {
			service.execute(lock(() -> {
				var key = new DebounceKey(e.getLevel(), e.getFormat());
				final var debounceLog = logs.computeIfAbsent(key, debounceKey -> new DebounceLog());
				debounceLog.add(e);
				final Runnable onComplete = lock(() -> {
					final var reduction = reducer.reduce(debounceLog.entries).toList();
					flusher.flush(reduction, logger);
					logs.remove(key);
				});
				debounceLog.debounce(onComplete);
				debounceLog.timeout(onComplete);
			}));
		}

		private Runnable lock(final Runnable runnable) {
			return () -> {
				lock.lock();
				try {
					runnable.run();
				} finally {
					lock.unlock();
				}
			};
		}

	}

	private record DebounceKey(Level level, String format) {

		@Override
		public boolean equals(final Object o) {
			return o instanceof final DebounceKey that && Objects.equals(level, that.level)
				&& Objects.equals(format, that.format);
		}

		@Override
		public int hashCode() {
			return Objects.hash(level, format);
		}

	}

	private class DebounceLog {

		private ScheduledFuture<?> debounceFuture;

		private ScheduledFuture<?> timeoutFuture;

		private final List<LogEntry> entries = new ArrayList<>();

		private void add(final LogEntry entry) {
			entries.add(entry);
		}

		private void debounce(final Runnable runnable) {
			if (debounceFuture != null) {
				debounceFuture.cancel(false);
			}

			// process logs immediately if queue size exceeds maximum
			final var localDelay =
				DebounceLogFactory.this.size() >= maxLogs ? 0 : debounceDelay.toNanos();

			debounceFuture = service.schedule(runnable, localDelay, TimeUnit.NANOSECONDS);
		}

		private void timeout(final Runnable runnable) {
			if (timeoutFuture != null && !timeoutFuture.isDone()) {
				return;
			}

			if (debounceFuture != null) {
				debounceFuture.cancel(false);
			}

			timeoutFuture = service.schedule(runnable, debounceTimeout.toNanos(),
				TimeUnit.NANOSECONDS);
		}

		private int size() {
			return entries.size();
		}

	}

}
