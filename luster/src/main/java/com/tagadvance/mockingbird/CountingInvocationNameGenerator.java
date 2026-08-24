package com.tagadvance.mockingbird;

import static java.util.Objects.requireNonNull;

import com.tagadvance.proxy.Invocation;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Names an invocation by a digest of what it is, plus how many times that same invocation has
 * already been seen.
 * <p>
 * The digest covers the method name, its declared return type, its parameter types and — under
 * {@link MatchType#ARGUMENTS} — the <em>serialized</em> arguments. Serialized, not hashed: an
 * argument type that does not override {@link Object#hashCode()} yields its identity hash, which
 * differs between two equal instances and between JVM runs, so a hash-derived name would never be
 * found again.
 * <p>
 * The counter is per digest, so adding an unrelated call elsewhere in a program does not renumber
 * anything.
 */
public final class CountingInvocationNameGenerator implements InvocationNameGenerator {

	private static final String EXTENSION = "gson";

	private static final String ALGORITHM = "SHA-256";

	/**
	 * Eight bytes of SHA-256 is ample for distinguishing the invocations of one interface, and
	 * keeps file names readable.
	 */
	private static final int DIGEST_BYTES = 8;

	private final MatchType matchType;

	private final MimicSerializer serializer;

	private final ConcurrentMap<String, AtomicInteger> countsByDigest = new ConcurrentHashMap<>();

	/**
	 * Alias of {@link #CountingInvocationNameGenerator(MatchType, MimicSerializer)} that defaults
	 * to {@link MatchType#ARGUMENTS} and JSON.
	 */
	public CountingInvocationNameGenerator() {
		this(MatchType.ARGUMENTS, new JsonMimicSerializer());
	}

	/**
	 * @param matchType  what the digest covers
	 * @param serializer used to render arguments deterministically
	 */
	public CountingInvocationNameGenerator(final MatchType matchType,
		final MimicSerializer serializer) {
		this.matchType = requireNonNull(matchType, "matchType must not be null");
		this.serializer = requireNonNull(serializer, "serializer must not be null");
	}

	@Override
	public String toName(final Class<?> iface, final Invocation invocation) {
		final var digest = digest(invocation);
		final var i = countsByDigest.computeIfAbsent(digest, key -> new AtomicInteger())
			.getAndIncrement();

		return "%s.%04d.%s".formatted(digest, i, EXTENSION);
	}

	String digest(final Invocation invocation) {
		final var method = invocation.method();
		final var digest = newDigest();
		update(digest, method.getName());
		update(digest, method.getGenericReturnType().getTypeName());
		Stream.of(method.getParameterTypes()).map(Class::getName).forEach(name -> update(digest, name));

		if (matchType == MatchType.ARGUMENTS) {
			update(digest, render(invocation.args()));
		}

		final var bytes = digest.digest();

		return HexFormat.of().formatHex(bytes, 0, DIGEST_BYTES);
	}

	private String render(final Object[] args) {
		final var writer = new StringWriter();
		try {
			serializer.write(writer, args, Object[].class);
		} catch (final Exception e) {
			throw new UncheckedIOException(new java.io.IOException(
				"arguments could not be serialized, so no stable name can be derived for them", e));
		}

		return writer.toString();
	}

	private static MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance(ALGORITHM);
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException("%s is required of every JVM".formatted(ALGORITHM), e);
		}
	}

	private static void update(final MessageDigest digest, final String value) {
		digest.update(value.getBytes(StandardCharsets.UTF_8));
		// a separator, so that concatenations of different fields cannot collide
		digest.update((byte) 0);
	}

	/**
	 * What a recording is matched on.
	 */
	public enum MatchType {

		/**
		 * Match on the method's signature only, ignoring argument values. Use this when an
		 * argument is inherently unstable, such as a timestamp or a request id.
		 */
		PARAMETER_TYPE,

		/**
		 * Match on the method's signature and its argument values.
		 */
		ARGUMENTS

	}

}
