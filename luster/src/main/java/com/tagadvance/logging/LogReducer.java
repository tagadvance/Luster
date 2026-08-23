package com.tagadvance.logging;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Reduces a {@link Collection collection} of logs in some way, e.g. removing duplicate entries or
 * pruning {@link StackTraceElement elements} from a {@link Throwable#getStackTrace() stack trace}.
 */
@FunctionalInterface
public interface LogReducer {

	/**
	 * Reduce log entries.
	 *
	 * @param logEntries a {@link Collection collection} of {@link LogEntry log entries}
	 * @return a reduced {@link Stream stream} of {@link LogEntry log entries}
	 */
	Stream<LogEntry> reduce(Collection<LogEntry> logEntries);

}
