package com.tagadvance.cache;

import java.util.Collection;
import java.util.Optional;

/**
 * Holds the proxy and the {@link Cache caches} behind it.
 *
 * @param <I> the proxied interface type
 */
public interface CacheController<I> {

	/**
	 * @return the proxy
	 */
	I proxy();

	/**
	 * Names are unique within a proxied interface, so this is deterministic.
	 *
	 * @param name the name declared by {@link CacheConfiguration#name()}
	 * @return an {@link Optional optional} {@link Cache cache}
	 */
	Optional<Cache> getCache(String name);

	/**
	 * @return every {@link Cache cache} behind this proxy
	 */
	Collection<Cache> caches();

}
