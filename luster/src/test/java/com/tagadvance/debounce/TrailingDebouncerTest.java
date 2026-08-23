package com.tagadvance.debounce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Tests for the trailing-edge {@link Debouncer}.
 */
class TrailingDebouncerTest {

	private static final Duration DELAY = Duration.ofMillis(100);

	private final ManualScheduler scheduler = new ManualScheduler();

	private final AtomicInteger calls = new AtomicInteger();

	private final Debouncer debouncer = Debouncer.trailing(scheduler, DELAY, calls::incrementAndGet);

	@Test
	void manySignalsInOneWindowFireOnce() {
		IntStream.range(0, 10).forEach(i -> {
			debouncer.signal();
			scheduler.advance(Duration.ofMillis(10));
		});

		assertEquals(0, calls.get(), "the window should not have closed yet");

		scheduler.advance(DELAY);

		assertEquals(1, calls.get());
	}

	@Test
	void signalsSpanningTwoWindowsFireTwice() {
		debouncer.signal();
		scheduler.advance(DELAY);

		debouncer.signal();
		scheduler.advance(DELAY);

		assertEquals(2, calls.get());
	}

	@Test
	void cancelBeforeTheWindowClosesFiresNothing() {
		debouncer.signal();
		scheduler.advance(Duration.ofMillis(50));

		debouncer.cancel();
		scheduler.advance(DELAY);

		assertEquals(0, calls.get());
		assertFalse(debouncer.isPending());
	}

	@Test
	void flushFiresImmediatelyAndTheScheduledFireDoesNot() {
		debouncer.signal();

		assertTrue(debouncer.flush());
		assertEquals(1, calls.get());

		scheduler.advance(DELAY);

		assertEquals(1, calls.get(), "the pending fire must not run as well");
	}

	@Test
	void flushWithNothingPendingDoesNothing() {
		assertFalse(debouncer.flush());
		assertEquals(0, calls.get());
	}

	@Test
	void isPendingTracksTheWindow() {
		assertFalse(debouncer.isPending());

		debouncer.signal();
		assertTrue(debouncer.isPending());

		scheduler.advance(DELAY);
		assertFalse(debouncer.isPending());
	}

	@Test
	void maxWaitFiresUnderAContinuousSignalStorm() {
		final var bounded = Debouncer.trailing(scheduler, DELAY, calls::incrementAndGet)
			.withMaxWait(Duration.ofMillis(300));

		// a signal every 50ms never lets the 100ms delay elapse
		IntStream.range(0, 6).forEach(i -> {
			bounded.signal();
			scheduler.advance(Duration.ofMillis(50));
		});

		assertEquals(1, calls.get(), "maxWait should have forced a fire");
	}

	@Test
	void maxWaitRestartsTheWindowAfterFiring() {
		final var bounded = Debouncer.trailing(scheduler, DELAY, calls::incrementAndGet)
			.withMaxWait(Duration.ofMillis(300));

		IntStream.range(0, 6).forEach(i -> {
			bounded.signal();
			scheduler.advance(Duration.ofMillis(50));
		});
		assertEquals(1, calls.get());

		bounded.signal();
		scheduler.advance(DELAY);

		assertEquals(2, calls.get());
	}

	@Test
	void everyFutureIsReleasedAfterFiring() {
		final var bounded = Debouncer.trailing(scheduler, DELAY, calls::incrementAndGet)
			.withMaxWait(Duration.ofMillis(300));
		bounded.signal();
		scheduler.advance(DELAY);

		assertEquals(1, calls.get());
		assertEquals(0, scheduler.pendingTaskCount(), "the maxWait future should have been cancelled");
	}

	@Test
	void aCallbackMaySignalWithoutRecursing() {
		final var reentrant = new AtomicInteger();
		final Debouncer[] holder = new Debouncer[1];
		holder[0] = Debouncer.trailing(scheduler, DELAY, () -> {
			if (reentrant.incrementAndGet() == 1) {
				holder[0].signal();
			}
		});

		holder[0].signal();
		scheduler.advance(DELAY);
		assertEquals(1, reentrant.get());

		scheduler.advance(DELAY);
		assertEquals(2, reentrant.get(), "the callback's own signal should open a new window");
	}

	@Test
	void aNonPositiveDelayIsRejected() {
		assertThrows(IllegalArgumentException.class,
			() -> Debouncer.trailing(scheduler, Duration.ZERO, calls::incrementAndGet));
	}

	@Test
	void concurrentSignalsFireExactlyOncePerWindow() throws Exception {
		final var realScheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			final var count = new AtomicInteger();
			final var concurrent = Debouncer.trailing(realScheduler, Duration.ofMillis(200),
				count::incrementAndGet);

			final var threads = 8;
			final var barrier = new CyclicBarrier(threads);
			final var executor = Executors.newFixedThreadPool(threads);
			try {
				final var futures = IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
					barrier.await();
					for (int j = 0; j < 100; j++) {
						concurrent.signal();
					}

					return null;
				})).toList();
				for (final var future : futures) {
					future.get(10, TimeUnit.SECONDS);
				}
			} finally {
				executor.shutdownNow();
			}

			Thread.sleep(600);

			assertEquals(1, count.get(), "800 signals in one window must produce one callback");
		} finally {
			realScheduler.shutdownNow();
		}
	}

}
