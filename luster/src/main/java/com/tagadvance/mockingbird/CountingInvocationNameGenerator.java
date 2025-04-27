package com.tagadvance.mockingbird;

import static java.util.Objects.requireNonNull;

import com.tagadvance.proxy.Invocation;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a {@link String name} from an {@link Invocation invocation}.
 */
public class CountingInvocationNameGenerator implements InvocationNameGenerator {

	private static final String EXTENSION = "gson";

	private final MatchType matchType;

	private final AtomicInteger count = new AtomicInteger();

	/**
	 * Alias of {@link #CountingInvocationNameGenerator(MatchType)} that defaults to
	 * {@link MatchType#ARGUMENTS}.
	 */
	public CountingInvocationNameGenerator() {
		this(MatchType.ARGUMENTS);
	}

	public CountingInvocationNameGenerator(final MatchType matchType) {
		this.matchType = requireNonNull(matchType, "matchType must not be null");
	}

	@Override
	public String toName(final Class<?> iface, final Invocation invocation) {
		final var name = iface.getSimpleName();
		final var hash = hash(invocation);
		final var i = count.getAndIncrement();

		return "%s.%s.%04d.%s".formatted(name, hash, i, EXTENSION);
	}

	String hash(final Invocation invocation) {
		final var method = invocation.method();
		final var hash = Objects.hash(method.getReturnType().getSimpleName(), method.getName(),
			toParameterOrArgumentHash(invocation));

		return Integer.toHexString(hash);
	}

	private int toParameterOrArgumentHash(final Invocation invocation) {
		switch (matchType) {
			case PARAMETER_TYPE -> {
				final var parameterTypes = invocation.method().getParameterTypes();

				return Arrays.hashCode(parameterTypes);
			}
			case ARGUMENTS -> {
				final var args = invocation.args();

				return Arrays.hashCode(args);
			}
			default -> throw new IllegalStateException(
				"Unsupported %s: %s".formatted(MatchType.class.getSimpleName(), matchType.name()));
		}
	}

	public enum MatchType {

		PARAMETER_TYPE,

		ARGUMENTS

	}

}
