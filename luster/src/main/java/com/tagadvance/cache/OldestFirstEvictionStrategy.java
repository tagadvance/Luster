package com.tagadvance.cache;

import java.util.Collection;
import java.util.Comparator;

public final class OldestFirstEvictionStrategy implements EvictionStrategy {

	@Override
	public void evict(final Collection<CacheEntry> entries, final int limit) {
		if (entries.size() <= limit) {
			return;
		}

		entries
			.stream()
			.sorted(Comparator.comparing(CacheEntry::creationTime).reversed())
			.map(CacheEntry::creationTime)
			.skip(limit - 1)
			.findFirst()
			.ifPresent(cutoff -> entries.removeIf(entry -> cutoff.isAfter(entry.creationTime())));
	}

}
