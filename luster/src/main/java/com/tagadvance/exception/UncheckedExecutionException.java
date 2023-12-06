package com.tagadvance.exception;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutionException;

/**
 * {@link UncheckedExecutionException} is essentially an unchecked alternative tp
 * {@link ExecutionException}.
 */
public class UncheckedExecutionException extends RuntimeException {

	/**
	 * Constructs a new {@link UncheckedExecutionException} with the specified detail message and
	 * cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public UncheckedExecutionException(final String message, final Throwable cause) {
		super(message, requireNonNull(cause, "cause must not be null"));
	}

	/**
	 * Constructs a new {@link UncheckedExecutionException} with the specified cause.
	 *
	 * @param cause the cause
	 */
	public UncheckedExecutionException(final Throwable cause) {
		super(requireNonNull(cause, "cause must not be null"));
	}

}
