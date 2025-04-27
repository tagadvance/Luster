package com.tagadvance.logging;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import org.slf4j.Logger;

/**
 * Persists queued {@link LogEntry log entries}.
 */
@FunctionalInterface
public interface LogFlusher {

	/**
	 * Flush queued log entries. Flushed log entries should be removed from the
	 * {@link Collection<LogEntry> collection}.
	 *
	 * @param logEntries a {@link Collection collection} of {@link LogEntry log entries}
	 * @param logger     a {@link Logger logger}
	 */
	default void flush(final Collection<LogEntry> logEntries, Logger logger) {
		final var toRemove = new HashSet<LogEntry>();
		try {
			flush(logEntries, logger, toRemove::add);
		} finally {
			toRemove.forEach(logEntries::remove);
		}
	}

	/**
	 * Flush queued log entries. Flushed log entries should be removed from the
	 * {@link Collection<LogEntry> collection}.
	 *
	 * @param logEntries a mutable collection of {@link LogEntry log entries}
	 * @param logger     a {@link Logger logger}
	 * @param remove     a callback that prevents future processing of the
	 *                   {@link LogEntry log entry}
	 */
	void flush(Collection<LogEntry> logEntries, Logger logger, Consumer<LogEntry> remove);

}
