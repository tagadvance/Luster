package com.tagadvance.cache;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * A {@link CacheConfiguration} after parsing and validation. The annotation is the declaration;
 * this is what the cache is actually built from.
 */
record CacheSettings(String name, Optional<Duration> expireAfterAccess,
					 Optional<Duration> expireAfterWrite, Optional<Duration> refreshAfterWrite,
					 int initialCapacity, long maximumSize, boolean recordStats,
					 boolean softValues) {

	static CacheSettings from(final CacheConfiguration configuration) {
		final var name = configuration.name();
		if (name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}

		if (configuration.initialCapacity() < 0) {
			throw new IllegalArgumentException(
				"%s: initialCapacity must not be negative".formatted(name));
		}

		return new CacheSettings(name, duration(name, "expireAfterAccess",
			configuration.expireAfterAccess()),
			duration(name, "expireAfterWrite", configuration.expireAfterWrite()),
			duration(name, "refreshAfterWrite", configuration.refreshAfterWrite()),
			configuration.initialCapacity(), configuration.maximumSize(),
			configuration.recordStats(), configuration.softValues());
	}

	private static Optional<Duration> duration(final String name, final String member,
		final String value) {
		if (value.isBlank()) {
			return Optional.empty();
		}

		final Duration duration;
		try {
			duration = Duration.parse(value);
		} catch (final DateTimeParseException e) {
			throw new IllegalArgumentException(
				"%s: %s is not an ISO-8601 duration: %s".formatted(name, member, value), e);
		}

		if (duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException(
				"%s: %s must be positive: %s".formatted(name, member, value));
		}

		return Optional.of(duration);
	}

}
