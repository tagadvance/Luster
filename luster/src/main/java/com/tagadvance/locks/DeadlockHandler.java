package com.tagadvance.locks;

import com.tagadvance.stack.StackTraces;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What to do when {@link DeadlockDetector} finds a cycle.
 */
@FunctionalInterface
public interface DeadlockHandler {

	/**
	 * @param deadlock the cycle that was found
	 */
	void onDeadlock(Deadlock deadlock);

	/**
	 * Logs the cycle at error level, with each thread's stack trace pruned through
	 * {@link StackTraces#JDK_PACKAGES}.
	 * <p>
	 * Deliberately does not try to break the cycle. The deadlocked threads are blocked and cannot
	 * throw, so the only way to act would be to interrupt one of them — which is a policy decision
	 * about which victim to sacrifice, and leaves that thread holding state its own code never
	 * expected to abandon.
	 *
	 * @return a handler that reports and does nothing else
	 */
	static DeadlockHandler logging() {
		final Logger logger = LoggerFactory.getLogger(DeadlockHandler.class);

		return deadlock -> logger.error("Deadlock detected: {}{}{}", deadlock,
			System.lineSeparator(), describe(deadlock, StackTraces.JDK_PACKAGES));
	}

	/**
	 * @param deadlock the cycle to render
	 * @param noise    frames matching this pattern are dropped from each stack trace
	 * @return one indented block per thread in the cycle
	 */
	static String describe(final Deadlock deadlock, final Pattern noise) {
		final var retain = StackTraces.retainElement(noise).negate();

		return deadlock.threads().stream().map(thread -> {
			final var frames = Stream.of(thread.getStackTrace())
				.filter(retain)
				.map(element -> "\t\tat " + element)
				.collect(Collectors.joining(System.lineSeparator()));

			return "\t\"%s\" %s%s%s".formatted(thread.getName(), thread.getState(),
				System.lineSeparator(), frames);
		}).collect(Collectors.joining(System.lineSeparator()));
	}

	/**
	 * @param handlers the handlers to combine
	 * @return a handler that invokes every one of {@literal handlers} in order
	 */
	static DeadlockHandler all(final DeadlockHandler... handlers) {
		final var copy = List.of(handlers);

		return deadlock -> copy.forEach(handler -> handler.onDeadlock(deadlock));
	}

}
