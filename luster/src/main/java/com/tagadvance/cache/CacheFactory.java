package com.tagadvance.cache;

public interface CacheFactory {

	<T, I extends T> CacheController<I> newCache(final Class<I> instanceType, final T instance);

}
