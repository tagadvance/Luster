package com.tagadvance.cache;

import java.util.List;
import java.util.Optional;

public interface CacheController<I> {

	/**
	 * @return the proxy
	 */
	I proxy();

	/**
	 * Attempts to retrieve the {@link Cache cache} with the specified name. If there are multiple
	 * caches with the same name then the result is non-deterministic.
	 *
	 * @param name the name of the cache as specified by {@link CacheConfiguration#name()}
	 * @return an {@link Optional optional} {@link Cache cache}
	 */
	default Optional<Cache> getCache(String name) {
		return getCaches(name).stream().findAny();
	}

	/**
	 * Attempts to retrieve the {@link Cache caches} with the specified name.
	 *
	 * @param name the name of the cache as specified by {@link CacheConfiguration#name()}
	 * @return a {@link List list} of {@link Cache caches}
	 */
	List<Cache> getCaches(String name);

}
