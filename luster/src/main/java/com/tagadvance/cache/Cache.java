package com.tagadvance.cache;

/**
 * A single method's cache.
 */
public interface Cache {

	/**
	 * @return this cache's name, as declared by {@link CacheConfiguration#name()}
	 */
	String name();

	/**
	 * @return the number of entries currently held
	 */
	long size();

	/**
	 * Discards every entry.
	 */
	void clear();

	/**
	 * Discards the entry for one set of arguments, if present.
	 *
	 * @param args the arguments the cached call was made with
	 */
	void invalidate(Object... args);

	/**
	 * @return a snapshot of this cache's {@link CacheStatistics statistics}
	 */
	CacheStatistics statistics();

}
