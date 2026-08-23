package com.tagadvance.cache;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

/**
 * The arguments a cached invocation was made with. Each {@link Cache} belongs to a single method,
 * so the arguments alone identify an entry.
 * <p>
 * The key holds strong references to the arguments, so they are pinned for the life of the entry,
 * and a mutable argument mutated after the call will not be found again.
 */
record CacheKey(Object[] args) {

	CacheKey {
		requireNonNull(args, "args must not be null");
	}

	@Override
	public boolean equals(final Object o) {
		return this == o || o instanceof final CacheKey key && Arrays.deepEquals(args, key.args);
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(args);
	}

	@Override
	public String toString() {
		return Arrays.deepToString(args);
	}

}
