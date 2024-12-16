package com.tagadvance.cache;

public interface Cache {

	/**
	 * @return this cache's name
	 */
	String name();

	/**
	 * @return the size of the cache
	 */
	int size();

	/**
	 * Clear the cache.
	 */
	void clear();

	/**
	 * @return the {@link CacheStatistics cache statistics}
	 */
	CacheStatistics statistics();

}
