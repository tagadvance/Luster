package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tagadvance.utilities.Patterns;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Unit tests for {@link PruneStackTraceLogFlusher}.
 */
class PruneStackTraceLogFlusherTest {

	@Test
	void test() {
		final var namespace = Patterns.compile("^com\\.tagadvance");
		final var flusher = new PruneStackTraceLogFlusher(namespace);
		final var exception = new Exception("foo");
		final var logEntries = List.of(new LogEntry(Level.INFO, "foo", exception));
		final var logger = mock(Logger.class);
		flusher.flush(logEntries, logger);

		final var stackTrace = exception.getStackTrace();
		assertEquals(1, stackTrace.length);
		assertTrue(
			Stream.of(stackTrace).allMatch(e -> e.getClassName().startsWith("com.tagadvance")));
	}

}
