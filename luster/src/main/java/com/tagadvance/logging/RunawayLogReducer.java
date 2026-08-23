package com.tagadvance.logging;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.event.Level;

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
public class RunawayLogReducer implements LogReducer {

	public RunawayLogReducer() {

	}

	@Override
	public Stream<LogEntry> reduce(final Collection<LogEntry> logEntries) {
		return logEntries.stream()
			.sorted(Comparator.comparing(LogEntry::getInstant))
			.collect(Collectors.groupingBy(RunawayLogReducer::hash))
			.entrySet()
			.stream()
			.flatMap(e -> {
				final var hash = e.getKey();
				final var logs = e.getValue();
				if (logs.size() == 1) {
					return logs.stream();
				} else if (hash == 0) {
					return reduceMessages(logs);
				} else {
					return reduceThrowables(logs);
				}
			});
	}

	private Stream<LogEntry> reduceMessages(final List<LogEntry> logs) {
		final var first = logs.stream()
			.min(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var last = logs.stream()
			.max(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var range = Duration.between(first.getInstant(), last.getInstant());

		final var limit = 3;
//		logger.error("Encountered {} duplicate log messages over a time period of {}, e.g. {} ",
//			logs.size(), range,
//			logs.stream().limit(limit).map(LogEntry::toString).collect(Collectors.joining(", ")));
		// FIXME
		return Stream.empty();
	}

	private Stream<LogEntry> reduceThrowables(final List<LogEntry> logs) {
		final var first = logs.stream()
			.min(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var last = logs.stream()
			.max(Comparator.comparing(LogEntry::getInstant))
			.orElseThrow();
		final var range = Duration.between(first.getInstant(), last.getInstant());

//		logger.error(
//			"Encountered {} duplicate log messages over a time period of {}, e.g. \"{}\" {}",
//			logs.size(), range, first, first.getThrowable().map(Throwable::toString).orElseThrow());
		// FIXME
		return Stream.empty();
	}

	private static int hash(final LogEntry logEntry) {
		return logEntry.getThrowable().map(RunawayLogReducer::hash).orElse(0);
	}

	private static int hash(final Throwable throwable) {
		final var stackTrace = throwable.getStackTrace();

		return Arrays.hashCode(stackTrace);
	}

}
