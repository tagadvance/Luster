package com.tagadvance.staging;

import java.util.concurrent.CancellationException;

/**
 * The cancellation signal handed to a staging body.
 * <p>
 * Luster deliberately does not ship a cancellation context. Mature ones already exist — gRPC's
 * {@code io.grpc.Context} is a standalone, dependency-free artifact usable without gRPC, and
 * {@code StructuredTaskScope} is the standard library's answer once the baseline allows it. This
 * is a one-method adapter so any of them, or none of them, can drive the split:
 *
 * <pre>{@code
 * Staged.stage(context::isCancelled, body -> ...);          // gRPC, or your own
 * Staged.stage(Staging.interrupted(), body -> ...);         // just the interrupt flag
 * Staged.stage(cancelled::get, body -> ...);                // an AtomicBoolean
 * }</pre>
 */
@FunctionalInterface
public interface Staging {

	/**
	 * @return a signal backed by the calling thread's interrupt status, for when there is no
	 * context in the picture at all
	 */
	static Staging interrupted() {
		return () -> Thread.currentThread().isInterrupted();
	}

	/**
	 * @return a signal that is never cancelled, chiefly useful in tests
	 */
	static Staging never() {
		return () -> false;
	}

	/**
	 * @return {@literal true} if the work should be abandoned
	 */
	boolean isCancelled();

	/**
	 * Abandons the staging body if it has been cancelled.
	 * <p>
	 * Throws rather than returning a boolean, so that forgetting to act on cancellation is an
	 * omission the compiler cannot hide behind an ignored return value.
	 *
	 * @throws CancellationException if cancelled
	 */
	default void checkCancelled() {
		if (isCancelled()) {
			throw new CancellationException("staging was cancelled");
		}
	}

}
