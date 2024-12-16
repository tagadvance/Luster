package com.tagadvance.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class DefaultEvictionStrategy implements EvictionStrategy {

	private final List<EvictionStrategy> strategies = Arrays.asList(
		new SoftValueEvictionStrategy(),
		new OldestFirstEvictionStrategy()
	);

	@Override
	public void evict(final Collection<CacheEntry> entries, final int limit) {
		strategies.forEach(strategy -> strategy.evict(entries, limit));
	}

}
