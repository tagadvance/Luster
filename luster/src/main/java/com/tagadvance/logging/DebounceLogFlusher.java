package com.tagadvance.logging;

import java.util.Collection;
import java.util.function.Consumer;
import org.slf4j.Logger;

/**
 * A comprehensive {@link LogFlusher flusher} that combines {@link PruneStackTraceLogFlusher},
 * {@link RunawayLogFlusher}, and {@link DefaultLogFlusher}.
 */
public final class DebounceLogFlusher implements LogFlusher {

	private final LogFlusher flusher;

	public DebounceLogFlusher(final PruneStackTraceLogFlusher flusher) {
		this.flusher = new CompositeLogFlusher(flusher, new RunawayLogFlusher(),
			new DefaultLogFlusher());
	}

	@Override
	public void flush(final Collection<LogEntry> logEntries, final Logger logger,
		final Consumer<LogEntry> remove) {
		flusher.flush(logEntries, logger, remove);
	}

}
