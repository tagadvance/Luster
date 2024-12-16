package com.tagadvance.cache;

import java.util.Collection;

public final class SoftValueEvictionStrategy implements EvictionStrategy {

	@Override
	public void evict(final Collection<CacheEntry> entries, final int limit) {
		entries.removeIf(entry -> entry.value() == null);
	}

}
