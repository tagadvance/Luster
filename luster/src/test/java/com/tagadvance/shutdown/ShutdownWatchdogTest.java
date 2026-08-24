package com.tagadvance.shutdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link ShutdownWatchdog}.
 */
@Timeout(30)
@SuppressWarnings("try")
class ShutdownWatchdogTest {

	private static final Duration SHORT = Duration.ofMillis(200);

	private final CountDownLatch release = new CountDownLatch(1);

	@AfterEach
	void releaseStragglers() {
		release.countDown();
	}

	@Test
	void aStragglerIsReportedWithItsStackPruned() throws Exception {
		straggler("straggler-1");
		final var reported = new CountDownLatch(1);
		final var report = new AtomicReference<String>();

		try (final var ignored = ShutdownWatchdog.armed(SHORT, text -> {
			report.set(text);
			reported.countDown();
		})) {
			assertTrue(reported.await(5, TimeUnit.SECONDS), "the watchdog never reported");
		}

		final var text = report.get();
		assertTrue(text.contains("straggler-1"), text);
		assertTrue(text.contains("ShutdownWatchdogTest"), "the application frame should survive: " + text);
		assertFalse(text.contains("java.util.concurrent.locks.LockSupport"),
			"JDK frames should have been pruned: " + text);
	}

	@Test
	void aCleanShutdownWithinTheBudgetReportsNothing() throws Exception {
		final var reports = new AtomicInteger();

		try (final var ignored = ShutdownWatchdog.armed(Duration.ofSeconds(30),
			text -> reports.incrementAndGet())) {
			// finishes immediately, well inside the budget
			assertTrue(true);
		}

		Thread.sleep(300);

		assertEquals(0, reports.get(), "disarming before expiry must cancel the report");
	}

	@Test
	void daemonThreadsAreExcludedByDefault() throws Exception {
		straggler("daemon-straggler", true);
		final var reported = new CountDownLatch(1);
		final var report = new AtomicReference<String>();

		try (final var ignored = ShutdownWatchdog.armed(SHORT, text -> {
			report.set(text);
			reported.countDown();
		})) {
			assertTrue(reported.await(5, TimeUnit.SECONDS));
		}

		assertFalse(report.get().contains("daemon-straggler"),
			"a daemon thread does not hold the JVM open, so it is not the problem: " + report.get());
	}

	@Test
	void aFilterNarrowsTheReport() throws Exception {
		straggler("wanted");
		straggler("unwanted");
		final var reported = new CountDownLatch(1);
		final var report = new AtomicReference<String>();

		try (final var ignored = ShutdownWatchdog.armed(SHORT, text -> {
			report.set(text);
			reported.countDown();
		}, thread -> thread.getName().equals("wanted"))) {
			assertTrue(reported.await(5, TimeUnit.SECONDS));
		}

		assertTrue(report.get().contains("wanted"), report.get());
		assertFalse(report.get().contains("unwanted"), report.get());
	}

	@Test
	void theWatchdogThreadIsADaemonSoItCannotHoldTheJvmOpen() throws Exception {
		try (final var ignored = ShutdownWatchdog.armed(Duration.ofSeconds(30), text -> {
		})) {
			final var watcher = Thread.getAllStackTraces()
				.keySet()
				.stream()
				.filter(thread -> "ShutdownWatchdog".equals(thread.getName()))
				.findFirst()
				.orElseThrow();

			assertTrue(watcher.isDaemon());
		}
	}

	@Test
	void closeWaitsForAnInFlightReport() throws Exception {
		straggler("slow-report");
		final var started = new CountDownLatch(1);
		final var finished = new AtomicInteger();

		final var watchdog = ShutdownWatchdog.armed(SHORT, text -> {
			started.countDown();
			try {
				Thread.sleep(300);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			finished.incrementAndGet();
		});

		assertTrue(started.await(5, TimeUnit.SECONDS), "the report never started");
		watchdog.close();

		assertEquals(1, finished.get(), "close must not return while the report is still going out");
	}

	@Test
	void aReportWithNothingToSaySaysSo() {
		final var text = ShutdownWatchdog.describe(SHORT, thread -> false);

		assertTrue(text.contains("No threads"), text);
	}

	private void straggler(final String name) {
		straggler(name, false);
	}

	private void straggler(final String name, final boolean daemon) {
		final var thread = new Thread(() -> {
			try {
				release.await();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, name);
		thread.setDaemon(daemon);
		thread.start();
	}

}
