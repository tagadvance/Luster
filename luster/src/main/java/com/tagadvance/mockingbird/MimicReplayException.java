package com.tagadvance.mockingbird;

/**
 * Thrown when a recording cannot be replayed: the cassette is missing in
 * {@link Mockingbird.Mode#REPLAY}, or it records a throwable that cannot be reconstructed.
 */
public class MimicReplayException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	MimicReplayException(final String message) {
		super(message);
	}

	MimicReplayException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
