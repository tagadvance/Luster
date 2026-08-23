package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tagadvance.debounce.ManualScheduler;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;

/**
 * Tests for {@link CoalescingLoggerFactory}.
 */
class CoalescingLoggerFactoryTest {

	private static final Duration QUIET = Duration.ofSeconds(6);

	private final ManualScheduler scheduler = new ManualScheduler();

	private final MutableClock clock = new MutableClock();

	private final RecordingLogger recorder = new RecordingLogger("com.example.Job");

	private final ILoggerFactory delegate = name -> recorder;

	private CoalescingLoggerFactory.Builder builder() {
		return CoalescingLoggerFactory.builder()
			.withDelegate(delegate)
			.withScheduler(scheduler)
			.withQuietPeriod(QUIET)
			.withMaxWait(Duration.ofMinutes(1))
			.withClock(clock);
	}

	@Test
	void theFirstOccurrenceIsLoggedImmediatelyAndSynchronously() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		logger.error("Failed to sync tenant {}", 41);

		assertEquals(1, recorder.events.size(), "the first event must not wait for a window");
		assertEquals("Failed to sync tenant {}", recorder.events.get(0).message());
		assertEquals(Thread.currentThread().getName(), recorder.events.get(0).thread(),
			"the first event must be logged on the calling thread");
	}

	@Test
	void repeatsAreCoalescedIntoOneSummary() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		IntStream.range(0, 1_000).forEach(i -> logger.error("Failed to sync tenant {}", i));

		assertEquals(1, recorder.events.size(), "only the first occurrence should have been logged");

		clock.advance(Duration.ofSeconds(58));
		scheduler.advance(QUIET);

		assertEquals(2, recorder.events.size());
		final var summary = recorder.events.get(1);
		assertEquals(Level.ERROR, summary.level(), "the summary keeps the level of the noise");
		assertEquals(999L, summary.args()[0]);
		assertEquals("Failed to sync tenant {}", summary.args()[1]);
		assertEquals(Duration.ofSeconds(58), summary.args()[2]);
	}

	@Test
	void theSummaryCarriesBoundedSamples() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		IntStream.range(0, 10).forEach(i -> logger.error("Failed to sync tenant {}", i));
		scheduler.advance(QUIET);

		final var detail = recorder.events.get(1).args()[4].toString();
		assertTrue(detail.contains("first=[1], [2], [3]"), detail);
		assertTrue(detail.contains("last=[7], [8], [9]"), detail);
	}

	@Test
	void distinctMessagesAreNeverCoalescedTogether() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		logger.error("Failed to sync tenant {}", 1);
		logger.error("Failed to load config");

		assertEquals(2, recorder.events.size());
		assertNotEquals(recorder.events.get(0).message(), recorder.events.get(1).message());
	}

	@Test
	void distinctLevelsAreNeverCoalescedTogether() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		logger.error("Something happened");
		logger.warn("Something happened");

		assertEquals(2, recorder.events.size());
	}

	@Test
	void distinctThrowableTypesAreNeverCoalescedTogether() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		logger.error("Failed", new IllegalStateException("a"));
		logger.error("Failed", new java.io.IOException("b"));

		assertEquals(2, recorder.events.size());
	}

	@Test
	void preFormattedMessagesCoalesce() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		IntStream.range(0, 500)
			.forEach(i -> logger.error("Failed to sync tenant " + i, new IllegalStateException()));

		assertEquals(1, recorder.events.size(),
			"concatenated messages must coalesce, not look like 500 distinct ones");

		scheduler.advance(QUIET);

		assertEquals(499L, recorder.events.get(1).args()[0]);
	}

	@Test
	void theNormalizerCanBeDisabled() {
		final Logger logger = builder().withMessageNormalizer(UnaryOperator.identity())
			.build()
			.getLogger("com.example.Job");

		IntStream.range(0, 5).forEach(i -> logger.error("Failed to sync tenant " + i));

		assertEquals(5, recorder.events.size(), "identity normalizer means nothing coalesces");
	}

	@Test
	void aBypassMarkerIsNeverCoalesced() {
		final var audit = MarkerFactory.getMarker("AUDIT");
		final Logger logger = builder().withBypassMarker(audit)
			.build()
			.getLogger("com.example.Job");

		IntStream.range(0, 5).forEach(i -> logger.info(audit, "user {} logged in", i));

		assertEquals(5, recorder.events.size());
	}

	@Test
	void aBypassMarkerDoesNotAffectOtherEvents() {
		final Logger logger = builder().withBypassMarker(MarkerFactory.getMarker("AUDIT"))
			.build()
			.getLogger("com.example.Job");

		IntStream.range(0, 5).forEach(i -> logger.info("routine {}", i));

		assertEquals(1, recorder.events.size());
	}

	@Test
	void theCallersThrowableIsNotMutated() {
		final Logger logger = builder().build().getLogger("com.example.Job");
		final var throwable = new IllegalStateException("boom");
		final var before = throwable.getStackTrace();

		logger.error("Failed", throwable);
		scheduler.advance(QUIET);

		assertTrue(Arrays.equals(before, throwable.getStackTrace()),
			"logging must not rewrite the caller's stack trace");
		assertSame(throwable, recorder.events.get(0).throwable());
	}

	@Test
	void callerDataIsPreservedThroughTheLocationAwareHook() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		logger.error("Failed");

		assertEquals("com.tagadvance.logging.CoalescingLogger", recorder.events.get(0).fqcn(),
			"the wrapper must name itself so the backend can skip its frames");
	}

	@Test
	void aWindowThatClosesQuietlyEmitsNoSummary() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		logger.error("Failed once");
		scheduler.advance(QUIET);

		assertEquals(1, recorder.events.size(), "a lone event needs no summary");
	}

	@Test
	void theNextEventAfterAWindowClosesIsLoggedInFull() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		logger.error("Failed {}", 1);
		logger.error("Failed {}", 2);
		scheduler.advance(QUIET);
		assertEquals(2, recorder.events.size());

		logger.error("Failed {}", 3);

		assertEquals(3, recorder.events.size());
		assertEquals("Failed {}", recorder.events.get(2).message());
	}

	@Test
	void maxWaitBoundsAContinuousStorm() {
		final Logger logger = builder().withMaxWait(Duration.ofSeconds(10))
			.build()
			.getLogger("com.example.Job");

		logger.error("Failed {}", 0);
		scheduler.advance(Duration.ofSeconds(5));
		// this repeat pushes the quiet period out to t=11, but maxWait fires at t=10
		logger.error("Failed {}", 1);
		scheduler.advance(Duration.ofSeconds(5));

		assertEquals(2, recorder.events.size(), "maxWait should have forced a summary");
		assertEquals(1L, recorder.events.get(1).args()[0]);
	}

	@Test
	void closeEmitsPendingSummaries() {
		final var factory = builder().build();
		final Logger logger = factory.getLogger("com.example.Job");

		logger.error("Failed {}", 1);
		logger.error("Failed {}", 2);
		assertEquals(1, recorder.events.size());

		factory.close();

		assertEquals(2, recorder.events.size(), "close must drain what is pending");
		assertEquals(1L, recorder.events.get(1).args()[0]);
	}

	@Test
	void loggersAreCachedByName() {
		final var factory = builder().build();

		assertSame(factory.getLogger("a"), factory.getLogger("a"));
		assertNotEquals(factory.getLogger("a"), factory.getLogger("b"));
	}

	@Test
	void isEnabledDelegatesToTheRealLogger() {
		final Logger logger = builder().build().getLogger("com.example.Job");

		assertTrue(logger.isErrorEnabled());
		assertTrue(logger.isTraceEnabled());
	}

}
