package com.tagadvance.cache;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Contains utility methods to spin up proxies when a {@link CacheController} is not needed.
 */
public final class Caches {

	public static <T, I extends T> I newCache(final Class<I> instanceType, final T instance) {
		return new DefaultCacheFactory().newCache(instanceType, instance).proxy();
	}

	public static <T, I extends T> I newCache(final ScheduledExecutorService executor,
		final Class<I> instanceType, final T instance) {
		return new DefaultCacheFactory(executor).newCache(instanceType, instance).proxy();
	}

	private Caches() {
	}

}
