package com.tagadvance.logging;

import java.util.Collection;
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
	void flush(final Collection<LogEntry> logEntries, Logger logger);

}
