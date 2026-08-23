package com.tagadvance.cache;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationInterceptor;
import com.tagadvance.proxy.InvocationProxy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Builds caching proxies. Every {@link CacheConfiguration annotated} method on the proxied
 * interface gets its own {@link Cache}, created and validated up front so that a malformed
 * duration or a duplicate name fails here rather than at first use.
 */
public final class DefaultCacheFactory implements CacheFactory {

	/**
	 * Stands in for a {@literal null} return value, which Guava's cache will not store.
	 */
	private static final Object NULL = new Object();

	@Override
	public <T, I extends T> CacheController<I> newCache(final Class<I> instanceType,
		final T instance) {
		requireNonNull(instanceType, "instanceType must not be null");
		requireNonNull(instance, "instance must not be null");

		final var cachesByName = new LinkedHashMap<String, OperationCache>();
		final var cachesByMethod = new HashMap<Method, OperationCache>();
		final var methods = List.of(instanceType.getMethods());
		for (final var method : methods) {
			// a bridge method carries a copy of the annotation; it is not a second cache
			if (method.isBridge() || method.isSynthetic()) {
				continue;
			}

			final var configuration = method.getAnnotation(CacheConfiguration.class);
			if (configuration == null) {
				continue;
			}

			final var settings = CacheSettings.from(configuration);
			final var cache = new OperationCache(settings, method, instance);
			final var previous = cachesByName.putIfAbsent(settings.name(), cache);
			if (previous != null) {
				throw new IllegalArgumentException(
					"%s declares more than one cache named \"%s\"".formatted(instanceType.getName(),
						settings.name()));
			}

			// the proxy may hand us a bridge method or another override of the same signature
			methods.stream()
				.filter(m -> signatureEquals(m, method) || configuration.equals(
					m.getAnnotation(CacheConfiguration.class)))
				.forEach(m -> cachesByMethod.put(m, cache));
		}

		final var interceptor = new ReadThroughOperation(Map.copyOf(cachesByMethod));
		final var proxy = InvocationProxy.createProxy(instanceType, instance, interceptor);

		return new DefaultCacheController<>(proxy, Map.copyOf(cachesByName));
	}

	private static boolean signatureEquals(final Method method, final Method otherMethod) {
		return method.getName().equals(otherMethod.getName()) && Arrays.equals(
			method.getParameterTypes(), otherMethod.getParameterTypes());
	}

	private record DefaultCacheController<I>(I proxy,
											 Map<String, OperationCache> cachesByName) implements
		CacheController<I> {

		@Override
		public Optional<Cache> getCache(final String name) {
			requireNonNull(name, "name must not be null");

			return Optional.ofNullable(cachesByName.get(name));
		}

		@Override
		public Collection<Cache> caches() {
			return List.copyOf(cachesByName.values());
		}

	}

	private record ReadThroughOperation(
		Map<Method, OperationCache> cachesByMethod) implements InvocationInterceptor {

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			final var cache = cachesByMethod.get(invocation.method());

			return cache == null ? PassiveOperation.getInstance().onInvocation(invocation)
				: cache.onInvocation(invocation);
		}

	}

	private static final class OperationCache implements Cache, InvocationInterceptor {

		private final CacheSettings settings;

		private final Method method;

		private final Object instance;

		private final LoadingCache<CacheKey, Object> cache;

		private OperationCache(final CacheSettings settings, final Method method,
			final Object instance) {
			this.settings = settings;
			this.method = CacheUtils.resolve(method, instance);
			this.instance = instance;
			this.cache = build(settings, new CacheLoader<>() {
				@Override
				public Object load(final CacheKey key) throws Exception {
					return OperationCache.this.load(key);
				}
			});
		}

		private static LoadingCache<CacheKey, Object> build(final CacheSettings settings,
			final CacheLoader<CacheKey, Object> loader) {
			final var builder = CacheBuilder.newBuilder()
				.initialCapacity(settings.initialCapacity());
			settings.expireAfterAccess().ifPresent(builder::expireAfterAccess);
			settings.expireAfterWrite().ifPresent(builder::expireAfterWrite);
			settings.refreshAfterWrite().ifPresent(builder::refreshAfterWrite);
			if (settings.maximumSize() >= 0) {
				builder.maximumSize(settings.maximumSize());
			}

			if (settings.softValues()) {
				builder.softValues();
			}

			if (settings.recordStats()) {
				builder.recordStats();
			}

			return builder.build(loader);
		}

		private Object load(final CacheKey key) throws Exception {
			try {
				final var value = CacheUtils.invoke(method, instance, key.args());

				return value == null ? NULL : value;
			} catch (final Exception | Error e) {
				throw e;
			} catch (final Throwable t) {
				throw new IllegalStateException(t);
			}
		}

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			try {
				final var value = cache.get(new CacheKey(invocation.args()));

				return value == NULL ? null : value;
			} catch (final ExecutionException | UncheckedExecutionException | ExecutionError e) {
				// what the real method threw, restored
				throw e.getCause();
			}
		}

		@Override
		public String name() {
			return settings.name();
		}

		@Override
		public long size() {
			cache.cleanUp();

			return cache.size();
		}

		@Override
		public void clear() {
			cache.invalidateAll();
		}

		@Override
		public void invalidate(final Object... args) {
			cache.invalidate(new CacheKey(args));
		}

		@Override
		public CacheStatistics statistics() {
			return CacheStatistics.from(cache.stats());
		}

	}

}
