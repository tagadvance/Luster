package com.tagadvance.utilities;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SleepTest {

	@Test
	@DisplayName("a sub-millisecond duration is not truncated to zero")
	void subMillisecond() throws InterruptedException {
		final var recorder = Sleep.recorder();
		recorder.sleep(Duration.ofNanos(999_999));
		recorder.sleep(500, MICROSECONDS);

		assertEquals(List.of(Duration.ofNanos(999_999), Duration.ofNanos(500_000)),
			recorder.durations());
	}

	@Test
	@DisplayName("ofThread actually sleeps for a sub-millisecond duration")
	void ofThreadSubMillisecond() throws InterruptedException {
		final var start = System.nanoTime();
		Sleep.ofThread().sleep(Duration.ofNanos(999_999));
		final var elapsed = System.nanoTime() - start;

		assertTrue(elapsed >= 500_000, "elapsed " + elapsed + "ns");
	}

	@Test
	@DisplayName("ofThread accepts a duration whose nanosecond part exceeds a millisecond")
	void ofThreadFractionalMilliseconds() throws InterruptedException {
		final var start = System.nanoTime();
		Sleep.ofThread().sleep(Duration.ofNanos(1_500_000));
		final var elapsed = System.nanoTime() - start;

		assertTrue(elapsed >= 1_000_000, "elapsed " + elapsed + "ns");
	}

	@Test
	@DisplayName("ofThread does not sleep for a non-positive duration")
	void ofThreadNonPositive() throws InterruptedException {
		final var sleep = Sleep.ofThread();
		sleep.sleep(Duration.ZERO);
		sleep.sleep(Duration.ofSeconds(-1));
	}

	@Test
	@DisplayName("disabled returns immediately")
	void disabled() {
		final var start = System.nanoTime();

		assertTrue(Sleep.disabled().slept(Duration.ofDays(1)));
		assertTrue(System.nanoTime() - start < Duration.ofSeconds(1).toNanos());
	}

	@Test
	@DisplayName("the recorder captures every requested duration in order")
	void recorder() {
		final var recorder = Sleep.recorder();
		recorder.slept(Duration.ofSeconds(1));
		recorder.slept(2, MILLISECONDS);
		recorder.slept(Duration.ofSeconds(4));

		assertEquals(List.of(Duration.ofSeconds(1), Duration.ofMillis(2), Duration.ofSeconds(4)),
			recorder.durations());

		recorder.reset();

		assertEquals(List.of(), recorder.durations());
	}

	@Test
	@DisplayName("the recorded durations are immutable")
	void recorderDurationsAreImmutable() {
		final var durations = Sleep.recorder().durations();

		assertThrows(UnsupportedOperationException.class, () -> durations.add(Duration.ZERO));
	}

	@Test
	@DisplayName("slept restores the interrupt status and returns false when interrupted")
	void sleptInterrupted() {
		Thread.currentThread().interrupt();
		try {
			assertFalse(Sleep.ofThread().slept(Duration.ofSeconds(30)));
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			// clear the interrupt so it does not leak into the next test
			Thread.interrupted();
		}
	}

	@Test
	@DisplayName("uninterruptible sleeps out the duration and re-asserts the interrupt")
	void uninterruptible() throws InterruptedException {
		final var timeout = Duration.ofMillis(20);

		Thread.currentThread().interrupt();
		final var start = System.nanoTime();
		try {
			Sleep.uninterruptible(Sleep.ofThread()).sleep(timeout);

			assertTrue(System.nanoTime() - start >= timeout.toNanos() / 2);
			assertTrue(Thread.currentThread().isInterrupted(), "interrupt was not re-asserted");
		} finally {
			Thread.interrupted();
		}
	}

}
