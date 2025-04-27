package com.tagadvance.logging;

import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationInterceptor;
import com.tagadvance.proxy.InvocationProxy;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebounceLogFactory implements ILoggerFactory {

	final Set<LogEntry> logQueue = Collections.synchronizedSet(new LinkedHashSet<>());

	private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

	private final ScheduledExecutorService service;

	private final Duration debounceDelay;

	private final int maxLogs;

	private final LogFlusher flusher;

	protected DebounceLogFactory(final ScheduledExecutorService service,
		final Duration debounceDelay, final int maxLogs, final LogFlusher flusher) {
		this.service = service;
		this.debounceDelay = debounceDelay;
		this.maxLogs = maxLogs;
		this.flusher = flusher;
	}

	@Override
	public Logger getLogger(final String name) {
		final var logger = LoggerFactory.getLogger(name);

		return InvocationProxy.createProxy(Logger.class, logger, new LogInterceptor(logger));
	}

	final class LogInterceptor implements InvocationInterceptor {

		private final Logger logger;

		LogInterceptor(final Logger logger) {
			this.logger = logger;
		}

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			final var logEntry = LogEntry.fromInvocation(invocation);
			if (logEntry.isPresent()) {
				logQueue.add(logEntry.get());
				debounce();

				return null;
			}

			return invocation.invoke();
		}

		private void debounce() {
			future.updateAndGet(f -> {
				if (f != null) {
					f.cancel(false);
				}

				// process logs immediately if queue size exceeds maximum
				final var localDelay = logQueue.size() >= maxLogs ? 0 : debounceDelay.toNanos();

				return service.schedule(() -> flusher.flush(logQueue, logger), localDelay,
					TimeUnit.NANOSECONDS);
			});
		}

	}

}
