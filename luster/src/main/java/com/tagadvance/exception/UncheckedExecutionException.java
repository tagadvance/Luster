package com.tagadvance.exception;

import java.util.concurrent.ExecutionException;

/**
 * {@link UncheckedExecutionException} is essentially an unchecked alternative tp
 * {@link ExecutionException}.
 */
public class UncheckedExecutionException extends RuntimeException {

	/**
	 * Constructs a new {@link UncheckedExecutionException} with the specified detail message.
	 *
	 * @param message the detail message
	 */
	public UncheckedExecutionException(final String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link UncheckedExecutionException} with the specified detail message and
	 * cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause (A null value is permitted, and indicates that the cause is
	 *                nonexistent or unknown.)
	 */
	public UncheckedExecutionException(final String message, final Exception cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new {@link UncheckedExecutionException} with the specified cause.
	 *
	 * @param cause the cause (A null value is permitted, and indicates that the cause is
	 *              nonexistent or unknown.)
	 */
	public UncheckedExecutionException(final Exception cause) {
		super(cause);
	}

	@Override
	public synchronized Exception getCause() {
		return (Exception) super.getCause();
	}

}
