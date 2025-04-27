package com.tagadvance.logging;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Unit tests for {@link RunawayLogFlusher}.
 */
class DefaultLogFlusherTest {

	@Test
	void test() {
		final var logEntries = IntStream.range(0, 5)
			.mapToObj(i -> newLogEntry())
			.collect(Collectors.toList());
		final var logger = LoggerFactory.getLogger(DefaultLogFlusherTest.class);
		new RunawayLogFlusher().flush(logEntries, logger);
	}

	private static LogEntry newLogEntry() {
		return new LogEntry(Level.INFO, "Format {}", "token");
	}

}
