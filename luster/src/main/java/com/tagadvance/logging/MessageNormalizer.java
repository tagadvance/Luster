package com.tagadvance.logging;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Collapses the parts of a message that look like values, so that callers who pre-format with
 * {@link String#format} or plain concatenation coalesce the same way slf4j-template callers do.
 * <p>
 * A proper template contains no literal values, so normalizing one is a no-op:
 * <pre>{@code
 * "Failed to sync tenant 41"   -> "Failed to sync tenant #"
 * "Failed to sync tenant 1523" -> "Failed to sync tenant #"
 * "Failed to sync tenant {}"   -> "Failed to sync tenant {}"
 * }</pre>
 * <p>
 * The cost is over-collapsing: {@code "HTTP 404"} and {@code "HTTP 500"} become one fingerprint.
 * Supply your own normalizer, or {@link UnaryOperator#identity()}, if that distinction matters.
 */
public final class MessageNormalizer {

	private static final String PLACEHOLDER = "#";

	/**
	 * Matched before {@link #HEX}, because a UUID's hyphens split it into groups too short for
	 * that pattern to recognise, which would collapse two UUIDs to different shapes.
	 */
	private static final Pattern UUID = Pattern.compile(
		"\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");

	/**
	 * A hex run long enough to be an identifier rather than a word.
	 */
	private static final Pattern HEX = Pattern.compile("\\b[0-9a-fA-F]{8,}\\b");

	private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

	/**
	 * The default: collapse UUIDs, then long hex runs, then numbers.
	 *
	 * @return a normalizer suitable for {@code withMessageNormalizer}
	 */
	public static UnaryOperator<String> collapsingValues() {
		return message -> {
			final var withoutUuids = UUID.matcher(message).replaceAll(PLACEHOLDER);
			final var withoutHex = HEX.matcher(withoutUuids).replaceAll(PLACEHOLDER);

			return NUMBER.matcher(withoutHex).replaceAll(PLACEHOLDER);
		};
	}

	private MessageNormalizer() {
	}

}
