package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ScheduleTest {

	/**
	 * Test my assumption that a long delay scheduled first would block subsequent shorter delays.
	 * Thankfully that doesn't appear to be the case.
	 */
	@Test
	void testThatScheduledTasksDoNotBlock() {
		final var executor = Executors.newSingleThreadScheduledExecutor();

		try {
			final var count = new AtomicLong();
			final var future3 = executor.schedule(
				() -> assertEquals(3, count.incrementAndGet()), 300,
				TimeUnit.MILLISECONDS);
			final var future2 = executor.schedule(
				() -> assertEquals(2, count.incrementAndGet()), 200,
				TimeUnit.MILLISECONDS);
			final var future1 = executor.schedule(
				() -> assertEquals(1, count.incrementAndGet()), 100,
				TimeUnit.MILLISECONDS);

			Stream.of(future1, future2, future3).forEach(future -> {
				try {
					future.get();
				} catch (final InterruptedException | ExecutionException e) {
					fail(e);
				}
			});
		} finally {
			executor.shutdown();
		}
	}

}
