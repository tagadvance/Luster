package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

/**
 * Unit tests for {@link RunawayLogReducer}.
 */
class RunawayLogReducerTest {

	@Test
	@Disabled("RunawayLogReducer.reduce() coalescing is unimplemented; see TODO/05-logging.md")
	void reduceCoalescesDuplicates() {
		final var logEntries = getLogEntries();

		final var reduced = new RunawayLogReducer().reduce(logEntries).toList();

		// "unique", plus one coalesced entry per duplicate group
		assertEquals(5, reduced.size());
	}

	private static List<LogEntry> getLogEntries() {
		final var exception = new Exception("foo");

		return List.of(new LogEntry(Level.INFO, "unique"), new LogEntry(Level.INFO, "foo"),
			new LogEntry(Level.INFO, "foo"), new LogEntry(Level.ERROR, "error", exception),
			new LogEntry(Level.ERROR, "error", exception),
			new LogEntry(Level.ERROR, "error", exception), new LogEntry(Level.INFO, "{}", "foo"),
			new LogEntry(Level.INFO, "{}", "foo"), new LogEntry(Level.INFO, "{}{}", "foo", "bar"),
			new LogEntry(Level.INFO, "{}{}", "foo", "bar"));
	}

}
