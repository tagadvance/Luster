package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DebounceLogFactoryBuilder}.
 */
class DebounceLogFactoryBuilderTest {

	@Test
	void testBuild() {
		final var factory = new DebounceLogFactoryBuilder().withScheduledExecutorService(
				Executors.newSingleThreadScheduledExecutor())
			.withDebounceDelay(Duration.ofSeconds(1))
			.withMaxLogs(10)
			.withLogFlusher(new RunawayLogFlusher())
			.build();

		assertNotNull(factory, "factory is null");
	}

}
