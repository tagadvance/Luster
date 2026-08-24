package com.tagadvance.staging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tagadvance.locks.Locks;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Staged}.
 */
class StagedTest {

	private final AtomicBoolean cancelled = new AtomicBoolean();

	private final Staging staging = cancelled::get;

	@AfterEach
	void clearInterrupt() {
		Thread.interrupted();
	}

	@Test
	void aStagedValueIsNotPublishedUntilCommitted() {
		final var published = new AtomicReference<String>();

		final var staged = Staged.stage(staging, ignored -> "value");

		assertEquals("value", staged.value());
		assertFalse(staged.isCommitted());
		assertEquals(null, published.get());

		staged.commit(published::set);

		assertEquals("value", published.get());
		assertTrue(staged.isCommitted());
	}

	@Test
	void cancellationDuringStagingPublishesNothing() {
		final var published = new AtomicReference<String>();
		final var reached = new AtomicInteger();

		assertThrows(CancellationException.class, () -> Staged.stage(staging, body -> {
			for (int i = 0; i < 10; i++) {
				body.checkCancelled();
				if (reached.incrementAndGet() == 3) {
					cancelled.set(true);
				}
			}

			return "value";
		}));

		assertEquals(3, reached.get(), "staging should stop at the first check after cancelling");
		assertEquals(null, published.get());
	}

	@Test
	void alreadyCancelledStagingNeverRuns() {
		cancelled.set(true);
		final var ran = new AtomicBoolean();

		assertThrows(CancellationException.class, () -> Staged.stage(staging, body -> {
			ran.set(true);

			return "value";
		}));

		assertFalse(ran.get(), "there is no point starting work that is already unwanted");
	}

	@Test
	void aReduceInStagingLeavesCommitWithOneThingToApply() {
		final var applied = new AtomicReference<Integer>();

		Staged.stage(staging, body -> List.of(1, 2, 3, 4).stream().peek(i -> body.checkCancelled())
			.reduce(0, Integer::sum)).commit(applied::set);

		assertEquals(10, applied.get());
	}

	@Test
	void aCheckedExceptionFromStagingPropagates() {
		final var thrown = new IOException("boom");

		assertSame(thrown, assertThrows(IOException.class, () -> Staged.stage(staging, body -> {
			throw thrown;
		})));
	}

	@Test
	void committingTwiceIsRejected() {
		final var staged = Staged.of("value");
		staged.commit(value -> {
		});

		assertThrows(IllegalStateException.class, () -> staged.commit(value -> {
		}));
	}

	/**
	 * The mechanism: an interrupt that arrived during staging is cleared for the duration of the
	 * commit and restored afterwards, so a commit that blocks is not torn in half by it.
	 */
	@Test
	void anInterruptFromStagingDoesNotTearTheCommit() {
		final var staged = Staged.of("value");
		final var interruptedInsideCommit = new AtomicBoolean(true);

		Thread.currentThread().interrupt();
		staged.commit(value -> interruptedInsideCommit.set(Thread.currentThread().isInterrupted()));

		assertFalse(interruptedInsideCommit.get(), "the commit must not see the flag");
		assertTrue(Thread.currentThread().isInterrupted(), "the flag must be restored afterwards");
	}

	@Test
	void theInterruptIsRestoredEvenWhenTheCommitThrows() {
		final var staged = Staged.of("value");

		Thread.currentThread().interrupt();

		assertThrows(IllegalStateException.class, () -> staged.commit(value -> {
			throw new IllegalStateException("boom");
		}));
		assertTrue(Thread.currentThread().isInterrupted());
	}

	@Test
	void anUninterruptedCommitLeavesTheFlagClear() {
		Staged.of("value").commit(value -> {
		});

		assertFalse(Thread.currentThread().isInterrupted());
	}

	@Test
	@SuppressWarnings("try")
	void aCommitThatNeedsExclusionComposesWithALock() {
		final var lock = Locks.newLock();
		final var published = new AtomicReference<String>();
		final var staged = Staged.stage(staging, ignored -> "value");

		try (final var ignored = lock.write()) {
			staged.commit(published::set);
		}

		assertEquals("value", published.get());
	}

	@Test
	void theInterruptedSignalTracksTheThreadFlag() {
		final var interrupted = Staging.interrupted();

		assertFalse(interrupted.isCancelled());

		Thread.currentThread().interrupt();

		assertTrue(interrupted.isCancelled());
		assertThrows(CancellationException.class, interrupted::checkCancelled);
	}

	@Test
	void theNeverSignalIsNeverCancelled() {
		Thread.currentThread().interrupt();

		assertFalse(Staging.never().isCancelled());
	}

}
