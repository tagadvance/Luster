package com.tagadvance.cache;

import static java.util.Objects.requireNonNull;

import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationInterceptor;
import com.tagadvance.proxy.InvocationProxy;
import com.tagadvance.reflection.M;
import com.tagadvance.reflection.ReflectionException;
import com.tagadvance.utilities.Benchmark;
import com.tagadvance.utilities.Once;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultCacheFactory implements CacheFactory {

	private static final Logger log = LoggerFactory.getLogger(DefaultCacheFactory.class);

	private final ScheduledExecutorService executor;

	public DefaultCacheFactory() {
		this(Executors.newSingleThreadScheduledExecutor());
	}

	public DefaultCacheFactory(final ScheduledExecutorService executor) {
		this.executor = requireNonNull(executor, "executor must not be null");
	}

	@Override
	public <T, I extends T> CacheController<I> newCache(final Class<I> instanceType,
		final T instance) {
		final var callback = new ReadThroughOperation();
		final var proxy = InvocationProxy.createProxy(instanceType, instance, callback);

		return new DefaultCacheController<>(callback, proxy);
	}

	private class DefaultCacheController<I> implements CacheController<I> {

		private final ReadThroughOperation callback;
		private final I proxy;

		public DefaultCacheController(final ReadThroughOperation callback, final I proxy) {
			this.callback = callback;
			this.proxy = proxy;
		}

		@Override
		public I proxy() {
			return this.proxy;
		}

		@Override
		public List<Cache> getCaches(final String name) {
			requireNonNull(name, "name must not be null");

			return callback.getCaches(name);
		}

	}

	private class ReadThroughOperation implements InvocationInterceptor {

		private final ConcurrentHashMap<Method, InvocationInterceptor> callbackByMethod = new ConcurrentHashMap<>();

		private ReadThroughOperation() {
		}

		private List<Cache> getCaches(final String name) {
			return callbackByMethod.values().stream().filter(value -> {
				if (value instanceof final Cache cache) {
					return Objects.equals(name, cache.name());
				}

				return false;
			}).map(value -> (Cache) value).toList();
		}

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			final var method = invocation.method();

			return callbackByMethod.computeIfAbsent(method, key -> M.getAnnotations(key)
				.filter(a -> a instanceof CacheConfiguration)
				.map(a -> (CacheConfiguration) a)
				.findFirst()
				.map(a -> (InvocationInterceptor) new OperationCache(a))
				.orElseGet(PassiveOperation::getInstance)).onInvocation(invocation);
		}

	}

	private class OperationCache implements Cache, InvocationInterceptor {

		private final CacheConfiguration cacheConfiguration;

		private final ConcurrentHashMap<CacheKey, CacheEntry> map;

		private final ConcurrentHashMap<CacheKey, ScheduledFuture<?>> afterAccessFutures;

		private final ConcurrentHashMap<CacheKey, ScheduledFuture<?>> afterWriteFutures;

		private final ConcurrentHashMap<CacheKey, ScheduledFuture<?>> refreshAfterWriteFutures;

		private final CacheEntryFactory factory;

		private final Supplier<CacheStatistics> statisticsSupplier = Once.supplier(
			CacheStatistics::new);

		private final EvictionStrategy evictionStrategy;

		public OperationCache(final CacheConfiguration cacheConfiguration) {
			this.cacheConfiguration = validateConfiguration(cacheConfiguration);
			final var initialCapacity = cacheConfiguration.initialCapacity();
			this.map = new ConcurrentHashMap<>(initialCapacity);
			this.afterAccessFutures = new ConcurrentHashMap<>(initialCapacity);
			this.afterWriteFutures = new ConcurrentHashMap<>(initialCapacity);
			this.refreshAfterWriteFutures = new ConcurrentHashMap<>(initialCapacity);
			this.factory = newCacheEntryFactory();
			final var evictionStrategy = cacheConfiguration.evictionStrategy();
			this.evictionStrategy = newEvictionStrategy(evictionStrategy);
		}

		private CacheConfiguration validateConfiguration(
			final CacheConfiguration cacheConfiguration) {
			final var refreshDelay = cacheConfiguration.refreshAfterWriteDelay();
			final var refreshTimeUnit = cacheConfiguration.refreshAfterWriteTimeUnit();
			final var refresh = refreshTimeUnit.toMicros(refreshDelay);

			final var writeDelay = cacheConfiguration.expireAfterWriteDelay();
			final var writeTimeUnit = cacheConfiguration.expireAfterWriteTimeUnit();
			final var write = writeTimeUnit.toMicros(writeDelay);

			if (refresh > 0 && write > 0) {
				log.warn(
					"Invalid cache configuration detected: `expireAfterWrite` and `refreshAfterWrite` are both set");
			}

			return cacheConfiguration;
		}

		private CacheEntryFactory newCacheEntryFactory() {
			return value -> cacheConfiguration.softValues() ? new SoftCacheEntry(value)
				: new DefaultCacheEntry(value);
		}

		private EvictionStrategy newEvictionStrategy(final Class<? extends EvictionStrategy> c) {
			try {
				return c.getDeclaredConstructor().newInstance();
			} catch (final InvocationTargetException | InstantiationException |
						   IllegalAccessException | NoSuchMethodException e) {
				final var message = "%s is missing public constructor with no arguments".formatted(
					c.getName());
				throw new ReflectionException(message, e);
			}
		}

		@Override
		public CacheConfiguration configuration() {
			return this.cacheConfiguration;
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public void clear() {
			map.keySet().removeIf(key -> {
				clearAllFutures(key);

				return true;
			});
		}

		private void clearAllFutures(final CacheKey key) {
			Stream.of(afterAccessFutures, afterWriteFutures, refreshAfterWriteFutures)
				.forEach(map -> {
					final var future = map.get(key);
					if (future != null) {
						future.cancel(false);
						map.remove(key);
					}
				});
		}

		@Override
		public CacheStatistics statistics() {
			return statisticsSupplier.get();
		}

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			final var method = invocation.method();
			final var args = invocation.args();
			final var cacheKey = new CacheKey(method, args);

			expireAfterAccess(cacheKey);

			try {
				return map.compute(cacheKey, (key, currentValue) -> {
					if (currentValue != null) {
						final var value = currentValue.value();
						if (value != null) {
							recordStats(CacheStatistics::hit);

							return currentValue;
						}
					}

					recordStats(CacheStatistics::miss);

					final var instance = invocation.instance();
					final Supplier<Object> supplier = createSupplier(method, args, instance);
					final var value = supplier.get();

					try {
						return factory.newCacheEntry(value);
					} finally {
						expireAfterWrite(key);

						refreshAfterWrite(key, supplier);
					}
				});
			} catch (final ReflectionException e) {
				throw CacheUtils.toValidException(e, method);
			} finally {
				runEviction();
			}
		}

		private Supplier<Object> createSupplier(final Method method, final Object[] args,
			final Object instance) {
			final var instanceClass = instance.getClass();
			final var matchingMethods = M.getMethods(instanceClass)
				.filter(m -> CacheUtils.methodSignatureEquals(m, method))
				.toList();
			final Supplier<Object> supplier = switch (matchingMethods.size()) {
				case 0 -> throw new ReflectionException("no matching method found",
					new IllegalArgumentException());
				case 1 -> {
					final var match = matchingMethods.get(0);

					yield () -> {
						match.trySetAccessible();

						return M.invoke(instance, args).apply(match);
					};
				}
				default ->
					throw new ReflectionException("ambiguous method %s".formatted(method.getName()),
						new IllegalArgumentException());
			};

			return cacheConfiguration.recordStats() ? () -> {
				try {
					return Benchmark.profile(supplier,
						duration -> recordStats(stats -> stats.loadSuccess(duration)));
				} catch (final RuntimeException e) {
					recordStats(CacheStatistics::loadException);

					throw e;
				}
			} : supplier;
		}

		private void expireAfterAccess(final CacheKey key) {
			final var delay = cacheConfiguration.expireAfterAccessDelay();
			if (delay < 0) {
				return;
			}

			afterAccessFutures.compute(key, (k, v) -> {
				if (v != null) {
					v.cancel(false);
				}

				final var unit = cacheConfiguration.expireAfterAccessTimeUnit();

				return executor.schedule(() -> evict(k), delay, unit);
			});
		}

		private void expireAfterWrite(final CacheKey key) {
			final var delay = cacheConfiguration.expireAfterWriteDelay();
			if (delay < 0) {
				return;
			}

			final var unit = cacheConfiguration.expireAfterWriteTimeUnit();
			afterWriteFutures.compute(key, (k, v) -> {
				if (v != null) {
					v.cancel(false);
				}

				return executor.schedule(() -> evict(k), delay, unit);
			});
		}

		private void evict(final CacheKey key) {
			map.remove(key);
			recordStats(CacheStatistics::eviction);
		}

		private void refreshAfterWrite(final CacheKey key, final Supplier<Object> supplier) {
			final var refreshDelay = cacheConfiguration.refreshAfterWriteDelay();
			if (refreshDelay < 0) {
				return;
			}

			final var unit = cacheConfiguration.refreshAfterWriteTimeUnit();
			refreshAfterWriteFutures.compute(key, (k, v) -> {
				if (v != null) {
					v.cancel(false);
				}

				return executor.schedule(() -> {
					try {
						final var value = supplier.get();
						final var record = factory.newCacheEntry(value);
						map.put(k, record);
					} catch (final ReflectionException e) {
						final var message = "Refresh after write failed! %s #%s(...)".formatted(
							cacheConfiguration.name(), key.getMethod().getName());
						log.warn(message, e);
					}
				}, refreshDelay, unit);
			});
		}

		private void recordStats(final Consumer<CacheStatistics> consumer) {
			if (cacheConfiguration.recordStats()) {
				final var stats = statisticsSupplier.get();
				consumer.accept(stats);
			}
		}

		private void runEviction() {
			final var values = map.values();
			final var limit = cacheConfiguration.maximumSize();
			evictionStrategy.evict(values, limit);
		}

	}

	@FunctionalInterface
	private interface CacheEntryFactory {

		CacheEntry newCacheEntry(final Object value);

	}

}
