package com.tagadvance.staging;

import static java.util.Objects.requireNonNull;

import com.tagadvance.exception.ThrowingFunction;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The result of a staging phase, waiting to be committed.
 * <p>
 * Work is split in two. <strong>Staging</strong> computes and publishes nothing, so it can be
 * abandoned at any {@link Staging#checkCancelled() check} with nothing to unwind.
 * <strong>Committing</strong> applies the result and is short enough that not interrupting it
 * costs nothing.
 *
 * <pre>{@code
 * final var staged = Staged.stage(context::isCancelled, staging -> tenants.stream()
 * 	.peek(tenant -> staging.checkCancelled())
 * 	.map(this::computeDelta)
 * 	.reduce(Delta.empty(), Delta::merge));
 *
 * staged.commit(delta -> this.snapshot = delta.applyTo(this.snapshot));
 * }</pre>
 *
 * <p><strong>This does not make a thread interruptible that was not already.</strong>
 * {@link Thread#interrupt()} unblocks only at defined interruption points, and will never
 * interrupt a CPU-bound loop, a {@literal synchronized} acquisition, a native call, or a read on a
 * plain socket. What the split buys is that abandoning at a checkpoint becomes <em>safe</em>.
 *
 * <p><strong>Staging reads are not isolated.</strong> If the staging body computes from shared
 * state that changes before the commit lands, the commit applies a decision derived from a stale
 * read. Re-check anything that can move inside the commit itself. Recording a read set and
 * validating it would be optimistic concurrency control, which brings retry policy and
 * side-effects-during-retry with it; a half-built version of that is worse than none, because it
 * looks like it protects you.
 *
 * @param <V> the type of the staged value
 */
public final class Staged<V> {

	private final V value;

	private final AtomicBoolean committed = new AtomicBoolean();

	/**
	 * Runs the staging body, which may be abandoned at any point it checks.
	 *
	 * @param staging the cancellation signal, also handed to {@literal body}
	 * @param body    the work to stage; it must publish nothing
	 * @param <V>     the type of the staged value
	 * @param <E>     the type of exception the body may throw
	 * @return the staged result, ready to commit
	 * @throws CancellationException if cancelled before or during staging
	 * @throws E                     if the body threw
	 */
	public static <V, E extends Exception> Staged<V> stage(final Staging staging,
		final ThrowingFunction<Staging, V, E> body) throws E {
		requireNonNull(staging, "staging must not be null");
		requireNonNull(body, "body must not be null");

		// no point starting expensive work that is already unwanted
		staging.checkCancelled();

		return new Staged<>(body.applyChecked(staging));
	}

	/**
	 * @param value the already-computed value to commit
	 * @param <V>   the type of the staged value
	 * @return a {@link Staged} holding {@literal value}
	 */
	public static <V> Staged<V> of(final V value) {
		return new Staged<>(value);
	}

	private Staged(final V value) {
		this.value = value;
	}

	/**
	 * @return the staged value, which has not been published anywhere
	 */
	public V value() {
		return value;
	}

	/**
	 * Applies the staged value.
	 * <p>
	 * An interrupt that arrived during staging is cleared for the duration and restored
	 * afterwards, so a commit that blocks is not torn in half by it. This does <em>not</em>
	 * prevent an interrupt arriving <em>during</em> the commit, and it is not mutual exclusion:
	 * where the commit must also exclude other threads, hold a lock around it.
	 *
	 * <pre>{@code
	 * try (final var ignored = lock.write()) {
	 * 	staged.commit(delta -> this.snapshot = delta);
	 * }
	 * }</pre>
	 *
	 * <p>If the commit throws, whatever it had already applied stays applied — this type defers
	 * interruption, it cannot make an arbitrary block atomic. Write the commit so that it is.
	 *
	 * @param commit applies the value; keep it short and non-blocking
	 * @throws IllegalStateException if already committed
	 */
	public void commit(final Consumer<? super V> commit) {
		requireNonNull(commit, "commit must not be null");
		if (!committed.compareAndSet(false, true)) {
			throw new IllegalStateException("already committed");
		}

		final var wasInterrupted = Thread.interrupted();
		try {
			commit.accept(value);
		} finally {
			if (wasInterrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * @return {@literal true} once {@link #commit(Consumer)} has been called
	 */
	public boolean isCommitted() {
		return committed.get();
	}

}
