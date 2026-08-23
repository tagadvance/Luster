package com.tagadvance.exception;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects every failure in a pipeline and throws once, at the end, with the remaining failures
 * attached as {@link Throwable#getSuppressed() suppressed}.
 * <pre>{@code
 * try (final var collector = ExceptionCollector.create()) {
 * 	tenants.forEach(collector.consumer(this::sync));
 * } // throws if any tenant failed
 * }</pre>
 * <p>
 * {@link #close()} throws {@link UncheckedException} rather than the collected exception itself,
 * so that try-with-resources does not force every caller to catch {@link Exception}. Wrap the
 * block in {@link Checked#rethrowing(Class, ThrowingRunnable)} to get the original type back.
 *
 * @see OnError for the handle-each-failure-immediately variant
 */
public final class ExceptionCollector implements OnError, AutoCloseable {

	private final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

	/**
	 * @return a new, empty {@link ExceptionCollector}
	 */
	public static ExceptionCollector create() {
		return new ExceptionCollector();
	}

	private ExceptionCollector() {
	}

	@Override
	public void handleException(final Exception e) {
		exceptions.add(requireNonNull(e, "e must not be null"));
	}

	/**
	 * @return the failures collected so far, in the order they occurred
	 */
	public List<Exception> getExceptions() {
		return List.copyOf(exceptions);
	}

	/**
	 * @throws UncheckedException wrapping the first collected failure, with the rest suppressed, if
	 *                            anything failed
	 */
	@Override
	public void close() throws UncheckedException {
		final var collected = getExceptions();
		if (collected.isEmpty()) {
			return;
		}

		final var e = new UncheckedException(collected.get(0));
		collected.stream().skip(1).forEach(e::addSuppressed);

		throw e;
	}

}
