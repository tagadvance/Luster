package com.tagadvance.reflection;

import static java.util.Objects.requireNonNull;

/**
 * {@link ReflectionException}.
 */
public class ReflectionException extends RuntimeException {

	/**
	 * Constructs a new {@link ReflectionException} with the specified detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public ReflectionException(final String message, final Exception cause) {
		super(message, requireNonNull(cause, "cause may not be null"));
	}

	/**
	 * Constructs a new {@link ReflectionException} with the specified cause.
	 *
	 * @param cause the cause
	 */
	public ReflectionException(final Exception cause) {
		super(requireNonNull(cause, "cause may not be null"));
	}

	/**
	 * @return the cause
	 */
	@Override
	public synchronized Exception getCause() {
		return (Exception) super.getCause();
	}

}
