package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebounceLogFactory}.
 */
class DebounceLogFactoryTest {

	@Test
	void testLoggerRunsImmediatelyAfterMaxLogsReached() throws Exception {
		final var service = Executors.newSingleThreadScheduledExecutor();
		final Duration debounceDelay = Duration.ofSeconds(1);
		final int maxLogs = 5;
		final AtomicBoolean wasFlushed = new AtomicBoolean(false);
		final LogFlusher flusher = (logs, logger, remove) -> {
			wasFlushed.set(true);

			assertEquals(maxLogs, logs.size());
		};
		final var factory = new DebounceLogFactory(service, debounceDelay, maxLogs, flusher);
		final var logger = factory.getLogger(DebounceLogFactoryTest.class.getName());
		logger.info("test");
		logger.info("test", new Exception("test"));
		logger.info("{}", "foo");
		logger.info("{}{}", "foo", "bar");
		logger.info("{}{}", "foo", "bar", new Exception("test"));

		Thread.sleep(1);
		service.shutdown();

		assertTrue(wasFlushed.get());
	}

}
