package com.tagadvance.cache;

public interface Cache {

	/**
	 * @return this cache's name
	 */
	String name();

	/**
	 * Clear the cache.
	 */
	void clear();

	/**
	 * @return the {@link CacheStatistics cache statistics}
	 */
	CacheStatistics statistics();

}
