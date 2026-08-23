package com.tagadvance.exception;

import static java.util.Objects.requireNonNull;

/**
 * An unchecked wrapper around a checked {@link Exception}.
 * <p>
 * The {@link Checked} adapters throw this when a checked exception escapes a lambda that the JDK
 * requires to throw nothing. Use {@link Checked#rethrowing(Class, ThrowingRunnable)} at the
 * boundary to unwrap it again.
 */
public class UncheckedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link UncheckedException} with the specified detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public UncheckedException(final String message, final Throwable cause) {
		super(message, requireNonNull(cause, "cause must not be null"));
	}

	/**
	 * Constructs a new {@link UncheckedException} with the specified cause.
	 *
	 * @param cause the cause
	 */
	public UncheckedException(final Throwable cause) {
		super(requireNonNull(cause, "cause must not be null"));
	}

}
