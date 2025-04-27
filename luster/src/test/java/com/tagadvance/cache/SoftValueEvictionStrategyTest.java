package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SoftValueEvictionStrategy}.
 */
class SoftValueEvictionStrategyTest {

	@Test
	void testThatNullValuesAreRemoved() {
		final List<CacheEntry> entries = new ArrayList<>();
		entries.add(newEntry(Instant.EPOCH, new Object()));
		entries.add(newEntry(Instant.EPOCH, null));

		new SoftValueEvictionStrategy().evict(entries, Integer.MAX_VALUE);

		assertTrue(entries.stream().noneMatch(e -> e.value() == null));
	}

	private static CacheEntry newEntry(final Instant creationTime, final Object value) {
		final var entry = mock(SoftCacheEntry.class);
		when(entry.creationTime()).thenReturn(creationTime);
		when(entry.value()).thenReturn(value);

		return entry;
	}

}
