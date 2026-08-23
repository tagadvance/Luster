package com.tagadvance.debounce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Tests for the leading-edge {@link Debouncer}.
 */
class LeadingDebouncerTest {

	private static final Duration INTERVAL = Duration.ofMillis(100);

	private final ManualScheduler scheduler = new ManualScheduler();

	private final AtomicInteger calls = new AtomicInteger();

	private final Debouncer debouncer = Debouncer.leading(scheduler, INTERVAL,
		calls::incrementAndGet);

	@Test
	void theFirstSignalFiresImmediately() {
		debouncer.signal();
		scheduler.runDueTasks();

		assertEquals(1, calls.get());
	}

	@Test
	void furtherSignalsInsideTheIntervalDoNotFireAgain() {
		debouncer.signal();
		scheduler.runDueTasks();

		IntStream.range(0, 5).forEach(i -> debouncer.signal());
		scheduler.runDueTasks();

		assertEquals(1, calls.get());
		assertTrue(debouncer.isPending(), "the suppressed signals should still be pending");
	}

	@Test
	void suppressedSignalsCoalesceIntoOneFireAtTheIntervalEnd() {
		debouncer.signal();
		scheduler.runDueTasks();

		IntStream.range(0, 5).forEach(i -> debouncer.signal());
		scheduler.advance(INTERVAL);

		assertEquals(2, calls.get(), "one leading fire plus one coalesced trailing fire");
		assertFalse(debouncer.isPending());
	}

	@Test
	void aQuietIntervalClosesTheWindow() {
		debouncer.signal();
		scheduler.runDueTasks();
		scheduler.advance(INTERVAL);

		assertEquals(1, calls.get());

		// the window closed, so the next signal leads again
		debouncer.signal();
		scheduler.runDueTasks();

		assertEquals(2, calls.get());
	}

	@Test
	void cancelDiscardsSuppressedSignals() {
		debouncer.signal();
		scheduler.runDueTasks();
		debouncer.signal();

		debouncer.cancel();
		scheduler.advance(INTERVAL);

		assertEquals(1, calls.get());
		assertFalse(debouncer.isPending());
	}

	@Test
	void flushFiresSuppressedSignalsImmediately() {
		debouncer.signal();
		scheduler.runDueTasks();
		debouncer.signal();

		assertTrue(debouncer.flush());
		assertEquals(2, calls.get());

		scheduler.advance(INTERVAL);

		assertEquals(2, calls.get(), "the interval fire must not run as well");
	}

	@Test
	void withMaxWaitIsRejected() {
		assertThrows(UnsupportedOperationException.class,
			() -> debouncer.withMaxWait(Duration.ofSeconds(1)));
	}

	@Test
	void aNonPositiveIntervalIsRejected() {
		assertThrows(IllegalArgumentException.class,
			() -> Debouncer.leading(scheduler, Duration.ZERO, calls::incrementAndGet));
	}

}
