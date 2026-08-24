package com.tagadvance.locks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link DeadlockDetector}.
 * <p>
 * The threads these tests deadlock are never recoverable, so each is a daemon and is abandoned
 * rather than joined.
 */
@Timeout(30)
@SuppressWarnings("try")
class DeadlockDetectorTest {

	private final LockRegistry registry = LockRegistry.create();

	private final ScopedLock first = Locks.instrumented(registry);

	private final ScopedLock second = Locks.instrumented(registry);

	private final List<Thread> abandoned = new java.util.ArrayList<>();

	@AfterEach
	void abandonDeadlockedThreads() {
		abandoned.forEach(Thread::interrupt);
	}

	/**
	 * The regression test for this whole feature: two threads each holding a read lock and each
	 * wanting the other's write lock are permanently deadlocked, and the JVM cannot see it.
	 */
	@Test
	void aReadLockCycleIsDetectedWhereTheJvmIsBlind() throws Exception {
		deadlock(false);

		assertNull(ManagementFactory.getThreadMXBean().findDeadlockedThreads(),
			"if the JVM has learned to see this, the registry has lost its reason to exist");

		final var found = DeadlockDetector.of(registry).detect();

		assertEquals(1, found.size(), "expected exactly one cycle");
		assertEquals(2, found.get(0).threads().size());
	}

	@Test
	void aWriteLockCycleIsDetected() throws Exception {
		deadlock(true);

		final var found = DeadlockDetector.of(registry).detect();

		assertEquals(1, found.size());
		assertEquals(2, found.get(0).threads().size());
	}

	@Test
	void aReadToWriteUpgradeIsDetectedAsASelfCycle() throws Exception {
		final var holding = new CountDownLatch(1);
		start("upgrader", () -> {
			try (final var ignored = first.read()) {
				holding.countDown();
				// ReentrantReadWriteLock refuses to upgrade, so this never returns
				first.write().close();
			}
		});
		assertTrue(holding.await(5, TimeUnit.SECONDS));
		Thread.sleep(200);

		final var found = DeadlockDetector.of(registry).detect();

		assertEquals(1, found.size());
		assertTrue(found.get(0).isUpgrade(), found.get(0).toString());
	}

	@Test
	void readersDoNotBlockReadersSoTheyAreNotACycle() throws Exception {
		final var holding = new CountDownLatch(2);
		final var release = new CountDownLatch(1);
		start("reader-1", () -> holdRead(first, holding, release));
		start("reader-2", () -> holdRead(first, holding, release));
		assertTrue(holding.await(5, TimeUnit.SECONDS));

		try {
			assertEquals(List.of(), DeadlockDetector.of(registry).detect(),
				"two readers of one lock exclude nobody");
		} finally {
			release.countDown();
		}
	}

	@Test
	void anUncontendedWorkloadReportsNothing() throws Exception {
		for (int i = 0; i < 100; i++) {
			try (final var ignored = first.read()) {
				assertTrue(true);
			}

			try (final var ignored = second.write()) {
				assertTrue(true);
			}
		}

		assertEquals(List.of(), DeadlockDetector.of(registry).detect());
	}

	@Test
	void aReleasedLockLeavesNoStaleEdge() throws Exception {
		try (final var ignored = first.write()) {
			assertTrue(true);
		}

		assertEquals(List.of(), DeadlockDetector.of(registry).detect());
		assertEquals(List.of(), List.copyOf(registry.waits().keySet()));
	}

	@Test
	void anUninstrumentedLockRecordsNothing() throws Exception {
		final var plain = Locks.newLock();

		try (final var ignored = plain.write()) {
			assertEquals(List.of(), List.copyOf(registry.waits().keySet()));
		}
	}

	@Test
	void theHandlerReceivesEachCycle() throws Exception {
		deadlock(false);
		final var seen = new AtomicReference<Deadlock>();

		DeadlockDetector.of(registry, seen::set).detect().forEach(seen::set);

		assertEquals(2, seen.get().threads().size());
	}

	@Test
	void theReportNamesEveryThreadInTheCycle() throws Exception {
		deadlock(false);

		final var found = DeadlockDetector.of(registry).detect().get(0);
		final var report = DeadlockHandler.describe(found, com.tagadvance.stack.StackTraces.JDK_PACKAGES);

		found.threads().forEach(thread -> assertTrue(report.contains(thread.getName()), report));
	}

	@Test
	void pollReportsOnASchedule() throws Exception {
		deadlock(false);
		final var reported = new CountDownLatch(1);
		final var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
		try {
			DeadlockDetector.of(registry, deadlock -> reported.countDown())
				.poll(scheduler, Duration.ofMillis(50));

			assertTrue(reported.await(5, TimeUnit.SECONDS));
		} finally {
			scheduler.shutdownNow();
		}
	}

	/**
	 * Leaves two threads deadlocked forever: each takes one lock, then wants the other.
	 *
	 * @param exclusive whether the first acquisition is a write lock, which is the case the JVM
	 *                  can already see
	 */
	private void deadlock(final boolean exclusive) throws InterruptedException {
		final var held = new CountDownLatch(2);

		start("A", () -> {
			try (final var ignored = exclusive ? first.write() : first.read()) {
				held.countDown();
				await(held);
				second.write().close();
			}
		});
		start("B", () -> {
			try (final var ignored = exclusive ? second.write() : second.read()) {
				held.countDown();
				await(held);
				first.write().close();
			}
		});

		assertTrue(held.await(5, TimeUnit.SECONDS));
		// let both reach the blocking acquisition
		Thread.sleep(300);
	}

	private void holdRead(final ScopedLock lock, final CountDownLatch holding,
		final CountDownLatch release) {
		try (final var ignored = lock.read()) {
			holding.countDown();
			await(release);
		}
	}

	private void start(final String name, final Runnable runnable) {
		final var thread = new Thread(runnable, name);
		thread.setDaemon(true);
		thread.start();
		abandoned.add(thread);
	}

	private static void await(final CountDownLatch latch) {
		try {
			latch.await();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
