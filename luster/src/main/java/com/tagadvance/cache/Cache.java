package com.tagadvance.cache;

public interface Cache {

	/**
	 * @return the {@link CacheConfiguration cache configuration}
	 */
	CacheConfiguration configuration();

	/**
	 * @return this cache's name
	 */
	default String name() {
		return configuration().name();
	}

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
