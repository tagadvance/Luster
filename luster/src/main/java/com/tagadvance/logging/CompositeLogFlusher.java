package com.tagadvance.logging;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;

/**
 * This class merges multiple {@link LogFlusher flushers}.
 */
public final class CompositeLogFlusher implements LogFlusher {

	private final LogFlusher[] flushers;

	public CompositeLogFlusher(final LogFlusher... flushers) {
		Stream.of(flushers).forEach(f -> Objects.requireNonNull(f, "flusher must not be null"));
		this.flushers = requireNonNull(flushers, "flushers must not be null");
	}

	@Override
	public void flush(final Collection<LogEntry> logEntries, final Logger logger,
		final Consumer<LogEntry> remove) {
		for (final LogFlusher f : flushers) {
			f.flush(logEntries, logger, remove);
		}
	}

}
