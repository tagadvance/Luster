package com.tagadvance.logging;

import static java.util.Objects.requireNonNull;

import com.tagadvance.stack.StackTraces;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.slf4j.Logger;

/**
 * This class prunes stack traces to remove entries outside the supplied namespace pattern.
 */
public final class PruneStackTraceLogFlusher implements LogFlusher {

	private final Pattern namespace;

	/**
	 * @param namespace a regular expression used to filter
	 *                  {@link StackTraceElement stack trace elements}
	 */
	public PruneStackTraceLogFlusher(final Pattern namespace) {
		this.namespace = requireNonNull(namespace, "namespace must not be null");
	}

	@Override
	public void flush(final Collection<LogEntry> logEntries, final Logger logger,
		final Consumer<LogEntry> remove) {
		Optional.ofNullable(namespace)
			.ifPresent(namespace -> logEntries.stream()
				.map(LogEntry::getThrowable)
				.flatMap(Optional::stream)
				.forEach(throwable -> StackTraces.retain(throwable, namespace)));
	}

}
