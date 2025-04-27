package com.tagadvance.logging;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * Let's say we have a job that copies data from a restful API for each tenant in our database.
 * Sometimes a copy will fail. We don't really know all the ways that it can fail as the API is
 * managed by an upstream vendor, making it difficult to differentiate critical exceptions from
 * insignificant exceptions. We don't want bad data from tenant A to prevent tenants B and C from
 * synchronizing, so we log the error message and continue on our merry way to the next tenant. It's
 * no big deal if this was a one-off exception. What if the vendor's database crashed and now all
 * the copy operations are failing? Oh, no! Our logs are rapidly filling with noise! This class is
 * designed to alleviate that pain by detecting duplicate log messages and stack traces and
 * coalescing them into something useful. Please note that this reduction is lossy.
 */
public class RunawayLogFlusher implements LogFlusher {

	public RunawayLogFlusher() {

	}

	@Override
	public void flush(final Collection<LogEntry> logEntries, final Logger logger,
		final Consumer<LogEntry> remove) {
		logEntries.stream()
			.sorted(Comparator.comparing(LogEntry::getInstant))
			.collect(Collectors.groupingBy(LogEntry::getFormat))
			.forEach((format, logs) -> flush(logs, logger, remove));
	}

	private void flush(final List<LogEntry> logs, final Logger logger,
		final Consumer<LogEntry> remove) {
		logs.stream()
			.collect(Collectors.groupingBy(RunawayLogFlusher::hash))
			.forEach((hash, logEntries) -> {
				try {
					if (logEntries.size() == 1) {
						flushUnique(logEntries, logger);
					} else if (hash == 0) {
						flushMessages(logEntries, logger);
					} else {
						flushThrowables(logEntries, logger);
					}
				} finally {
					logs.forEach(remove);
				}
			});
	}

	private void flushUnique(final List<LogEntry> logs, final Logger logger) {
		logs.stream().findFirst().ifPresent(logEntry -> logEntry.log(logger));
	}

	private void flushMessages(final List<LogEntry> logs, final Logger logger) {
		final var first = logs.stream()
			.min(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var last = logs.stream()
			.max(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var range = Duration.between(first.getInstant(), last.getInstant());

		final var limit = 3;
		// TODO: replace error with highest log level
		logger.error("Encountered {} duplicate log messages over a time period of {}, e.g. {} ",
			logs.size(), range,
			logs.stream().limit(limit).map(LogEntry::toString).collect(Collectors.joining(", ")));
	}

	private void flushThrowables(final List<LogEntry> logs, final Logger logger) {
		final var first = logs.stream()
			.min(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var last = logs.stream()
			.max(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var range = Duration.between(first.getInstant(), last.getInstant());

		logger.error(
			"Encountered {} duplicate log messages over a time period of {}, e.g. \"{}\" {}",
			logs.size(), range, first, first.getThrowable().map(Throwable::toString).orElseThrow());
	}

	private static int hash(final LogEntry logEntry) {
		return logEntry.getThrowable().map(RunawayLogFlusher::hash).orElse(0);
	}

	private static int hash(final Throwable throwable) {
		final var stackTrace = throwable.getStackTrace();

		return Arrays.hashCode(stackTrace);
	}

}
