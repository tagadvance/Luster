package com.tagadvance.cache;

/**
 * Builds caching proxies.
 */
public interface CacheFactory {

	/**
	 * Wraps {@literal instance} in a proxy that caches every
	 * {@link CacheConfiguration annotated} method on {@literal instanceType}.
	 *
	 * @param instanceType the interface to proxy
	 * @param instance     the instance to delegate to
	 * @param <T>          the instance type
	 * @param <I>          the interface type
	 * @return a {@link CacheController} holding the proxy and its caches
	 */
	<T, I extends T> CacheController<I> newCache(final Class<I> instanceType, final T instance);

}
