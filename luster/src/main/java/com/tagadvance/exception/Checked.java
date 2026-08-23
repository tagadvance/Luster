package com.tagadvance.exception;

import static java.util.Objects.requireNonNull;

/**
 * The boundary that restores checked exceptions erased by the {@code Checked*} adapters.
 * <p>
 * The adapters exist to get a checked-exception-throwing lambda past a JDK signature that forbids
 * one; they rethrow as {@link UncheckedException}. This class puts the original exception back on
 * the wire at exactly one place, where the compiler can enforce the {@code throws} clause again.
 *
 * <pre>{@code
 * void foo() throws IOException {
 * 	Checked.rethrowing(IOException.class, () -> Stream.of(paths)
 * 		.map(CheckedFunction.of(Files::readString))
 * 		.forEach(System.err::println));
 * }
 * }</pre>
 */
public final class Checked {

	/**
	 * Runs the supplied body. If it fails with an {@link UncheckedException} whose cause is an
	 * {@link E}, that cause is rethrown unchanged; any other {@link UncheckedException} propagates
	 * as-is.
	 * <p>
	 * Only one exception type can be restored per call, because {@code throws E} cannot be
	 * expressed over a varargs of type tokens. Nest calls if you need two.
	 *
	 * @param type the exception type to restore
	 * @param body the body to run
	 * @param <E>  the type of exception that may be thrown
	 * @throws E if the body failed with a wrapped {@link E}
	 */
	public static <E extends Exception> void rethrowing(final Class<E> type,
		final ThrowingRunnable<RuntimeException> body) throws E {
		requireNonNull(type, "type must not be null");
		requireNonNull(body, "body must not be null");

		try {
			body.runChecked();
		} catch (final UncheckedException e) {
			throw unwrap(type, e);
		}
	}

	/**
	 * Returns the result of the supplied body. If it fails with an {@link UncheckedException} whose
	 * cause is an {@link E}, that cause is rethrown unchanged; any other {@link UncheckedException}
	 * propagates as-is.
	 *
	 * @param type the exception type to restore
	 * @param body the body to run
	 * @param <V>  the type of the result
	 * @param <E>  the type of exception that may be thrown
	 * @return the result of the body
	 * @throws E if the body failed with a wrapped {@link E}
	 */
	public static <V, E extends Exception> V rethrowing(final Class<E> type,
		final ThrowingSupplier<V, RuntimeException> body) throws E {
		requireNonNull(type, "type must not be null");
		requireNonNull(body, "body must not be null");

		try {
			return body.get();
		} catch (final UncheckedException e) {
			throw unwrap(type, e);
		}
	}

	/**
	 * Throws the cause as an {@link E} when it matches, otherwise returns the wrapper for the
	 * caller to throw. The return type lets both call sites read {@code throw unwrap(...)}.
	 */
	private static <E extends Exception> UncheckedException unwrap(final Class<E> type,
		final UncheckedException e) throws E {
		final var cause = e.getCause();
		if (type.isInstance(cause)) {
			throw type.cast(cause);
		}

		return e;
	}

	private Checked() {
	}

}
