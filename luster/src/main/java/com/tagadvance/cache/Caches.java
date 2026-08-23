package com.tagadvance.cache;

/**
 * Contains utility methods to spin up proxies when a {@link CacheController} is not needed.
 */
public final class Caches {

	/**
	 * @param instanceType the interface to proxy
	 * @param instance     the instance to delegate to
	 * @param <T>          the instance type
	 * @param <I>          the interface type
	 * @return the proxy
	 */
	public static <T, I extends T> I newCache(final Class<I> instanceType, final T instance) {
		return new DefaultCacheFactory().newCache(instanceType, instance).proxy();
	}

	private Caches() {
	}

}
