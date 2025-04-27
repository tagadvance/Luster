package com.tagadvance.stack;

import com.tagadvance.utilities.Patterns;
import java.util.function.Predicate;
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
		return e -> Patterns.compile(classNameRegex).matcher(e.getClassName()).find();
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
