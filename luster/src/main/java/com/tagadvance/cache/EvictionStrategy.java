package com.tagadvance.cache;

import java.util.Collection;

public interface EvictionStrategy {

	void evict(final Collection<CacheEntry> entries, final int limit);

}
