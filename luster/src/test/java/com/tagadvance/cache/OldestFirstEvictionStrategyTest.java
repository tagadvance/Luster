package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OldestFirstEvictionStrategy}.
 */
class OldestFirstEvictionStrategyTest {

	@Test
	void testThatOldestEntriesAreEvictedFirst() {
		final int limit = 10;
		final var expectedValue = "epoch";

		final var oldestEntry = newEntry(Instant.EPOCH, expectedValue);
		final List<CacheEntry> entries = IntStream.range(0, limit)
			.mapToObj(i -> newEntry(Instant.now(), expectedValue))
			.collect(Collectors.toList());
		entries.add(0, oldestEntry);
		entries.add(oldestEntry);

		new OldestFirstEvictionStrategy().evict(entries, limit);

		assertEquals(limit, entries.size());
		assertTrue(entries.stream().noneMatch(e -> e.creationTime().equals(Instant.EPOCH)));
	}

	private static CacheEntry newEntry(final Instant creationTime, final Object value) {
		final var entry = mock(CacheEntry.class);
		when(entry.creationTime()).thenReturn(creationTime);
		when(entry.value()).thenReturn(value);

		return entry;
	}

}
