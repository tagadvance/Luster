package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tagadvance.utilities.Patterns;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

/**
 * Unit tests for {@link PruneStackTraceLogReducer}.
 */
class PruneStackTraceLogReducerTest {

	@Test
	void test() {
		final var namespace = Patterns.compile("^com\\.tagadvance");
		final var reducer = new PruneStackTraceLogReducer(namespace);
		final var exception = new Exception("foo");
		final var logEntries = List.of(new LogEntry(Level.INFO, "foo", exception));
		// reduce() prunes via peek, so the stream must be consumed
		assertEquals(logEntries, reducer.reduce(logEntries).toList());

		final var stackTrace = exception.getStackTrace();
		assertEquals(1, stackTrace.length);
		assertTrue(
			Stream.of(stackTrace).allMatch(e -> e.getClassName().startsWith("com.tagadvance")));
	}

}
