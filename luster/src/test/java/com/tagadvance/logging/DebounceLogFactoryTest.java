package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebounceLogFactory}.
 */
class DebounceLogFactoryTest {

	@Test
	void testLoggerRunsImmediatelyAfterMaxLogsReached() throws Exception {
		final var service = Executors.newSingleThreadScheduledExecutor();
		// long enough that only the maxLogs path can flush within the await below
		final Duration debounceDelay = Duration.ofMinutes(1);
		final Duration debounceTimeout = Duration.ofMinutes(5);
		final int maxLogs = 5;
		final AtomicBoolean wasReduced = new AtomicBoolean(false);
		final LogReducer reducer = (logs) -> {
			wasReduced.set(true);

			return logs.stream();
		};
		final var flushed = new CountDownLatch(1);
		final var flushedSize = new AtomicInteger();
		final LogFlusher flusher = (logs, logger) -> {
			flushedSize.set(logs.size());
			flushed.countDown();
		};
		final var factory = new DebounceLogFactory(service, debounceDelay, debounceTimeout, maxLogs,
			reducer, flusher);
		final var logger = factory.getLogger(DebounceLogFactoryTest.class.getName());
		// the format must contain "{}" or the entry is passed straight through, not debounced
		for (int i = 0; i < maxLogs; i++) {
			logger.info("{}", i);
		}

		try {
			assertTrue(flushed.await(5, TimeUnit.SECONDS), "logs were not flushed immediately");
			assertTrue(wasReduced.get(), "logs were not reduced");
			assertEquals(maxLogs, flushedSize.get());
		} finally {
			service.shutdownNow();
		}
	}

}
