package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

/**
 * Unit tests for {@link RunawayLogFlusher}.
 */
class RunawayLogFlusherTest {

	@Test
	void test() {
		final var logEntries = getLogEntries();

		final var logCount = new AtomicInteger();
		final var logger = new LoggerAdapter() {
			@Override
			public void error(final String format, final Object... arguments) {
				switch (logCount.getAndIncrement()) {
					case 0:
						assertEquals(
							"Encountered {} duplicate log messages over a time period of {}, e.g. {} ",
							format);
						assertEquals(2, arguments[0]);
						assertInstanceOf(Duration.class, arguments[1]);
						assertEquals("foo, foo", arguments[2]);
						break;
					case 1:
						assertEquals(
							"Encountered {} duplicate log messages over a time period of {}, e.g. {}",
							format.trim());
						assertEquals(2, arguments[0]);
						assertInstanceOf(Duration.class, arguments[1]);
						assertEquals("foo, foo", arguments[2]);
						break;
					case 2:
						assertEquals(
							"Encountered {} duplicate log messages over a time period of {}, e.g. \"{}\" {}",
							format.trim());
						assertEquals(3, arguments[0]);
						assertInstanceOf(Duration.class, arguments[1]);
						assertInstanceOf(LogEntry.class, arguments[2]);
						break;
					case 3:
						assertEquals(
							"Encountered {} duplicate log messages over a time period of {}, e.g. {} ",
							format);
						assertEquals(2, arguments[0]);
						assertInstanceOf(Duration.class, arguments[1]);
						assertEquals("foobar, foobar", arguments[2]);
						break;
					case 4:
						fail("too many log messages");
						break;
				}
			}
		};

		final var removeCount = new AtomicInteger();
		new RunawayLogFlusher().flush(logEntries, logger, e -> removeCount.incrementAndGet());

		assertEquals(logEntries.size(), removeCount.get());
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
