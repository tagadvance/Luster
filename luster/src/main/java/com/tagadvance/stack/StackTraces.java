package com.tagadvance.stack;

import com.tagadvance.utilities.Patterns;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * {@link StackTraces} contains convenience methods for retrieving the
 * {@link Thread#getStackTrace() current stack trace} as a {@link Stream}.
 */
public class StackTraces {

	/**
	 * @return the {@link Thread#getStackTrace() current stack trace} as a {@link Stream} with
	 * elements from <a
	 * href="https://docs.oracle.com/en/java/javase/17/docs/api/allpackages-index.html">All
	 * Packages</a> removed
	 */
	public static Stream<StackTraceElement> asStream() {
		final var stackTrace = Thread.currentThread().getStackTrace();
		final var regex = "^(com\\.sun|java|javax|jdk|org\\.w3c\\.dom|org\\.xml\\.sax)\\.";

		return Stream.of(stackTrace).filter(retain(regex).negate()).skip(1);
	}

	/**
	 * @param classNameRegex a regular expression
	 * @return a {@link Predicate filter} that retains
	 * {@link StackTraceElement stack trace elements} where the
	 * {@link StackTraceElement#getClassName() class name} is found in the supplied input
	 */
	public static Predicate<StackTraceElement> retain(final String classNameRegex) {
		final var pattern = Patterns.compile(classNameRegex);

		return retain(pattern);
	}

	/**
	 * @param pattern a {@link Pattern pattern}
	 * @return a {@link Predicate filter} that retains
	 * {@link StackTraceElement stack trace elements} where the
	 * {@link StackTraceElement#getClassName() class name} is found in the supplied input
	 */
	public static Predicate<StackTraceElement> retain(final Pattern pattern) {
		return e -> pattern.matcher(e.getClassName()).find();
	}

	/**
	 * Recursively prunes the {@link Throwable#getStackTrace() stack trace} to remove
	 * {@link StackTraceElement elements} that do not patch the supplied {@link Pattern pattern}.
	 *
	 * @param throwable a {@link Throwable throwable}
	 * @param pattern   a {@link Pattern pattern} {@link StackTraceElement stack trace elements}
	 *                  where the {@link StackTraceElement#getClassName() class name} is found in
	 *                  the supplied input
	 */
	// TODO: unit test
	public static void retain(final Throwable throwable, final Pattern pattern) {
		var stackTrace = throwable.getStackTrace();
		final var prunedStackTrace = Stream.of(stackTrace)
			.filter(retain(pattern))
			.toArray(StackTraceElement[]::new);
		throwable.setStackTrace(prunedStackTrace);

		final var cause = throwable.getCause();
		if (cause != null) {
			retain(cause, pattern);
		}
	}

	/**
	 * @return {@literal true} if this code is being called from a JUnit test
	 */
	public static boolean isTesting() {
		return asStream().anyMatch(retain("org.junit.*"));
	}

	private StackTraces() {
	}

}
