package com.tagadvance.reflection;

import static java.util.Objects.requireNonNull;

/**
 * {@link ReflectionException}.
 */
public class ReflectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

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
	 * Not {@literal synchronized}: {@link Throwable#getCause()} already is, so this override would
	 * only take the same monitor a second time, reentrantly.
	 *
	 * @return the cause
	 */
	@Override
	public Exception getCause() {
		return (Exception) super.getCause();
	}

}
