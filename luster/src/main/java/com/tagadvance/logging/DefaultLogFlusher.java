package com.tagadvance.logging;

import java.util.Collection;
import java.util.function.Consumer;
import org.slf4j.Logger;

/**
 * Flushes unmodified logs.
 */
public final class DefaultLogFlusher implements LogFlusher {

	public DefaultLogFlusher() {
	}

	@Override
	public void flush(final Collection<LogEntry> logEntries, final Logger logger,
		final Consumer<LogEntry> remove) {
		logEntries.stream().peek(e -> e.log(logger)).forEach(remove);
	}

}
