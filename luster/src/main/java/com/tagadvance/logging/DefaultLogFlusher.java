package com.tagadvance.logging;

import java.util.Collection;
import org.slf4j.Logger;

/**
 * Flushes unmodified logs.
 */
public final class DefaultLogFlusher implements LogFlusher {

	public DefaultLogFlusher() {
	}

	@Override
	public void flush(final Collection<LogEntry> logEntries, final Logger logger) {
		logEntries.forEach(e -> e.log(logger));
	}

}
