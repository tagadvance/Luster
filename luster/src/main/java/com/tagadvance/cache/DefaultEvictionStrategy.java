package com.tagadvance.cache;

import java.util.Collection;

public final class DefaultEvictionStrategy implements EvictionStrategy {

	private final EvictionStrategy evictionStrategy;

	public DefaultEvictionStrategy() {
		this.evictionStrategy = new CompoundEvictionStrategy(
			new SoftValueEvictionStrategy(),
			new OldestFirstEvictionStrategy()
		);
	}

	@Override
	public void evict(final Collection<CacheEntry> entries, final int limit) {
		evictionStrategy.evict(entries, limit);
	}

}
