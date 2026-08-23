package com.tagadvance.locks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ScopedLock} and {@link Locks}.
 */
// the closeable-lock idiom never references the resource in the body -- that is the point of
// it -- and there is no way to write it that satisfies -Xlint:try
@SuppressWarnings("try")
class ScopedLockTest {

	private final ReentrantReadWriteLock delegate = new ReentrantReadWriteLock();

	private final ScopedLock lock = Locks.wrap(delegate);

	@Test
	void closingTheHandleReleasesTheReadLock() {
		try (final var ignored = lock.read()) {
			assertEquals(1, delegate.getReadLockCount());
		}

		assertEquals(0, delegate.getReadLockCount());
	}

	@Test
	void closingTheHandleReleasesTheWriteLock() {
		try (final var ignored = lock.write()) {
			assertTrue(delegate.isWriteLocked());
		}

		assertFalse(delegate.isWriteLocked());
	}

	@Test
	void aThrowingBodyStillReleasesTheHandle() {
		assertThrows(IOException.class, () -> {
			try (final var ignored = lock.write()) {
				throw new IOException("boom");
			}
		});

		assertFalse(delegate.isWriteLocked());
	}

	@Test
	void aThrowingCallbackStillReleasesTheLock() {
		final var thrown = new IOException("boom");

		assertSame(thrown, assertThrows(IOException.class, () -> lock.writeLock(() -> {
			throw thrown;
		})));

		assertFalse(delegate.isWriteLocked());
	}

	@Test
	void aCallbackRuntimeExceptionPropagatesUnwrapped() {
		final var thrown = new IllegalStateException("boom");

		assertSame(thrown, assertThrows(IllegalStateException.class, () -> lock.readLock(() -> {
			throw thrown;
		})));

		assertEquals(0, delegate.getReadLockCount());
	}

	@Test
	void aCallbackReturnsItsValue() throws Exception {
		assertEquals("value", lock.readLock(() -> "value"));
		assertEquals(0, delegate.getReadLockCount());
	}

	@Test
	void handlesAreReentrant() {
		try (final var outer = lock.read(); final var inner = lock.read()) {
			assertEquals(2, delegate.getReadLockCount());
		}

		assertEquals(0, delegate.getReadLockCount());
	}

	@Test
	void readersDoNotExcludeEachOther() throws Exception {
		final var held = new CountDownLatch(1);
		final var release = new CountDownLatch(1);
		final var executor = Executors.newSingleThreadExecutor();
		try {
			executor.submit(() -> {
				try (final var ignored = lock.read()) {
					held.countDown();
					release.await();
				}

				return null;
			});

			assertTrue(held.await(5, TimeUnit.SECONDS));

			try (final var ignored = lock.read()) {
				assertEquals(2, delegate.getReadLockCount(), "both readers should hold the lock");
			} finally {
				release.countDown();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void tryWriteGivesUpWhileAReaderHoldsTheLock() throws Exception {
		final var held = new CountDownLatch(1);
		final var release = new CountDownLatch(1);
		final var executor = Executors.newSingleThreadExecutor();
		try {
			executor.submit(() -> {
				try (final var ignored = lock.read()) {
					held.countDown();
					release.await();
				}

				return null;
			});

			assertTrue(held.await(5, TimeUnit.SECONDS));

			try {
				assertTrue(lock.tryWrite(Duration.ofMillis(100)).isEmpty(),
					"the write lock should not have been available");
			} finally {
				release.countDown();
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void tryWriteAcquiresAnUncontendedLock() throws Exception {
		final var handle = lock.tryWrite(Duration.ofSeconds(1));

		assertTrue(handle.isPresent());
		try (final var ignored = handle.orElseThrow()) {
			assertTrue(delegate.isWriteLocked());
		}

		assertFalse(delegate.isWriteLocked());
	}

	@Test
	void newLockIsReentrant() throws Exception {
		final var lock = Locks.newLock();

		try (final var outer = lock.write(); final var inner = lock.write()) {
			assertEquals("value", lock.readLock(() -> "value"));
		}
	}

}
