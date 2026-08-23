package com.tagadvance.logging;

import static java.util.Objects.requireNonNull;

import com.tagadvance.stack.StackTraces;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This class prunes stack traces to remove entries outside the supplied namespace pattern.
 */
public final class PruneStackTraceLogReducer implements LogReducer {

	private final Pattern namespace;

	/**
	 * @param namespace a regular expression used to filter
	 *                  {@link StackTraceElement stack trace elements}
	 */
	public PruneStackTraceLogReducer(final Pattern namespace) {
		this.namespace = requireNonNull(namespace, "namespace must not be null");
	}

	@Override
	public Stream<LogEntry> reduce(final Collection<LogEntry> logEntries) {
		return logEntries.stream()
			.peek(e -> e.getThrowable()
				.ifPresent(throwable -> StackTraces.retain(throwable, namespace)));
	}

}
