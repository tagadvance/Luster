package com.tagadvance.logging;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/**
 * What makes two log calls "the same" for the purpose of coalescing.
 * <p>
 * Arguments are deliberately excluded: coalescing {@code "Failed to sync tenant {}"} across every
 * tenant id is the entire point.
 *
 * @param loggerName     the name of the logger the call was made on
 * @param level          the level it was logged at
 * @param format         the message pattern, before argument substitution
 * @param throwableChain the classes of the throwable and its causes, outermost first
 */
record Fingerprint(String loggerName, Level level, String format, List<Class<?>> throwableChain) {

	/**
	 * A cause chain longer than this is truncated, which also bounds the work done on a
	 * self-referential chain.
	 */
	private static final int MAX_CAUSE_DEPTH = 16;

	static Fingerprint of(final String loggerName, final Level level, final @Nullable String format,
		final @Nullable Throwable throwable) {
		return new Fingerprint(loggerName, level, format == null ? "" : format, chainOf(throwable));
	}

	private static List<Class<?>> chainOf(final @Nullable Throwable throwable) {
		if (throwable == null) {
			return List.of();
		}

		final var chain = new ArrayList<Class<?>>();
		var current = throwable;
		while (current != null && chain.size() < MAX_CAUSE_DEPTH) {
			chain.add(current.getClass());
			final var cause = current.getCause();
			// a throwable may be its own cause
			current = cause == current ? null : cause;
		}

		return List.copyOf(chain);
	}

}
