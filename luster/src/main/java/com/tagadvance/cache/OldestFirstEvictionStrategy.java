package com.tagadvance.cache;

import java.util.Collection;

public final class OldestFirstEvictionStrategy implements EvictionStrategy {

	@Override
	public void evict(final Collection<CacheEntry> entries, final int limit) {
		if (entries.size() <= limit) {
			return;
		}

		final var cutoff = entries
			.stream()
			.map(CacheEntry::creationTime)
			.sorted()
			.toList()
			.get(limit - 1);

		entries.removeIf(entry -> cutoff.isBefore(entry.creationTime()));
	}

}
