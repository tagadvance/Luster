package com.tagadvance.cache;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Throwables;
import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationCallback;
import com.tagadvance.proxy.InvocationProxy;
import com.tagadvance.reflection.M;
import com.tagadvance.reflection.ReflectionException;
import com.tagadvance.utilities.Benchmark;
import com.tagadvance.utilities.Once;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class CacheFactory {

	private final ScheduledExecutorService executor;

	public CacheFactory() {
		this(Executors.newSingleThreadScheduledExecutor());
	}

	public CacheFactory(final ScheduledExecutorService executor) {
		this.executor = requireNonNull(executor, "executor must not be null");
	}

	public <T, I extends T> CacheController<I> newCache(final Class<I> instanceType,
		final T instance) {
		final var callback = new InterfaceCallback();
		final var proxy = InvocationProxy.createProxy(instanceType, instance, callback);

		return new CacheController<I>() {
			@Override
			public I proxy() {
				return proxy;
			}

			@Override
			public List<Cache> getCaches(final String name) {
				requireNonNull(name, "name must not be null");

				return callback.callbackByMethod.values()
					.stream()
					.filter(value -> {
						if (value instanceof final Cache cache) {
							return Objects.equals(name, cache.name());
						}

						return false;
					})
					.map(value -> (Cache) value)
					.toList();
			}

		};
	}

	private record CacheKey(Method method, Object[] args) {

		private CacheKey(final Method method, final Object[] args) {
			this.method = requireNonNull(method, "method must not be null");
			this.args = requireNonNull(args, "args must not be null");
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}

			if (o instanceof final CacheKey key) {
				return methodSignatureEquals(method, key.method)
					&& Objects.deepEquals(args, key.args);
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(methodHashCode(method), Arrays.hashCode(args));
		}

	}

	private class InterfaceCallback implements InvocationCallback {

		private final Supplier<InvocationCallback> supplier = Once.supplier(
			PassiveMethodCallback::new);

		private final ConcurrentHashMap<Method, InvocationCallback> callbackByMethod = new ConcurrentHashMap<>();

		private InterfaceCallback() {
		}

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			final var method = invocation.method();

			return callbackByMethod.computeIfAbsent(method, key -> M.getAnnotations(key)
					.filter(a -> a instanceof CacheConfiguration)
					.map(a -> (CacheConfiguration) a)
					.findFirst()
					.map(a -> (InvocationCallback) new CacheMethodCallback(a))
					.orElseGet(supplier))
				.onInvocation(invocation);
		}

	}

	private static class PassiveMethodCallback implements InvocationCallback {

		private PassiveMethodCallback() {

		}

		@Override
		public Object onInvocation(final Invocation invocation) throws Throwable {
			final var i = invocation.instance();
			final var method = invocation.method();
			final var args = invocation.args();

			try {
				return M.invoke(i, args).apply(method);
			} catch (final ReflectionException e) {
				throw toValidException(e, method);
			}
		}

	}

	private class CacheMethodCallback<I> implements Cache, InvocationCallback {

		private final CacheConfiguration cacheConfiguration;

		private final ConcurrentHashMap<CacheKey, Object> map;

		private final ConcurrentHashMap<CacheKey, ScheduledFuture<?>> afterAccessFutures;

		private final ConcurrentHashMap<CacheKey, ScheduledFuture<?>> afterWriteFutures;

		private final ConcurrentHashMap<CacheKey, ScheduledFuture<?>> refreshFutures;

		private final Supplier<CacheStatistics> statisticsSupplier = Once.supplier(
			CacheStatistics::new);

		public CacheMethodCallback(final CacheConfiguration cacheConfiguration) {
			this.cacheConfiguration = cacheConfiguration;
			final var initialCapacity = cacheConfiguration.initialCapacity();
			this.map = new ConcurrentHashMap<>(initialCapacity);
			this.afterAccessFutures = new ConcurrentHashMap<>(initialCapacity);
			this.afterWriteFutures = new ConcurrentHashMap<>(initialCapacity);
			this.refreshFutures = new ConcurrentHashMap<>(initialCapacity);
		}

		@Override
		public String name() {
			return cacheConfiguration.name();
		}

		@Override
		public void clear() {
			map.keySet().removeIf(key -> {
				Stream.of(afterAccessFutures, afterWriteFutures, refreshFutures)
					.forEach(map -> {
						final var future = map.get(key);
						if (future != null) {
							future.cancel(false);
							map.remove(key);
						}
					});

				return true;
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
						recordStats(CacheStatistics::hit);

						return currentValue;
					}

					recordStats(CacheStatistics::miss);

					final var instance = invocation.instance();
					final Supplier<Object> supplier = createSupplier(method, args, instance);
					var value = supplier.get();

					expireAfterWrite(key);

					refresh(key, supplier);

					return value;
				});
			} catch (final ReflectionException e) {
				throw toValidException(e, method);
			}
		}

		private Supplier<Object> createSupplier(final Method method, final Object[] args,
			final Object instance) {
			final var instanceClass = instance.getClass();
			final var matchingMethods = M.getMethods(instanceClass)
				.filter(m -> methodSignatureEquals(m, method))
				.toList();
			final Supplier<Object> supplier = switch (matchingMethods.size()) {
				case 0 -> throw new ReflectionException("no matching method found",
					new IllegalArgumentException());
				case 1 -> {
					final var match = matchingMethods.get(0);

					yield () -> M.invoke(instance, args).apply(match);
				}
				default -> throw new ReflectionException("ambiguous method",
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

			afterAccessFutures.computeIfPresent(key, (k, v) -> {
				v.cancel(false);
				return null;
			});

			afterAccessFutures.compute(key, (k, v) -> {
				final var unit = cacheConfiguration.expireAfterAccessTimeUnit();
				return executor.schedule(() -> evict(k), delay, unit);
			});
		}

		private void expireAfterWrite(final CacheKey key) {
			final var delay = cacheConfiguration.expireAfterWriteDelay();
			if (delay < 0) {
				return;
			}

			afterWriteFutures.computeIfPresent(key,
				(k, v) -> {
					v.cancel(false);
					return null;
				});

			final var unit = cacheConfiguration.expireAfterWriteTimeUnit();
			afterWriteFutures.compute(key,
				(k, v) -> executor.schedule(() -> evict(k), delay, unit));
		}

		private void evict(final CacheKey key) {
			map.remove(key);
			recordStats(CacheStatistics::eviction);
		}

		private void refresh(final CacheKey key, final Supplier<Object> supplier) {
			final var refreshDelay = cacheConfiguration.refreshAfterWriteDelay();
			if (refreshDelay < 0) {
				return;
			}

			final var writeDelay = cacheConfiguration.expireAfterWriteDelay();
			if (writeDelay < 1) {
				// TODO: log warning that this is in invalid configuration
			}

			refreshFutures.computeIfPresent(key,
				(k, v) -> {
					v.cancel(false);
					return null;
				});

			final var unit = cacheConfiguration.refreshAfterWriteTimeUnit();
			refreshFutures.compute(key, (k, v) -> executor.schedule(() -> {
					try {
						final var value = supplier.get();
						map.put(k, value);
					} catch (final ReflectionException e) {
						// FIXME: log refresh failure
					}
				},
				refreshDelay, unit));
		}

		private void recordStats(final Consumer<CacheStatistics> consumer) {
			if (cacheConfiguration.recordStats()) {
				final var stats = statisticsSupplier.get();
				consumer.accept(stats);
			}
		}

	}

	private static Throwable toValidException(final ReflectionException e, final Method method) {
		return Throwables.getCausalChain(e)
			.stream()
			.skip(1)
			.filter(exceptionMatchesMethodSignature(method))
			.findFirst()
			.orElse(e);
	}

	private static Predicate<Throwable> exceptionMatchesMethodSignature(final Method method) {
		final var exceptionTypes = method.getExceptionTypes();

		return t -> t instanceof RuntimeException
			|| Stream.of(exceptionTypes)
			.anyMatch(et -> et.isInstance(t));
	}

	private static int methodHashCode(final Method method) {
		return Objects.hash(method.getReturnType(),
			Arrays.hashCode(method.getParameterTypes()));
	}

	private static boolean methodSignatureEquals(final Method method,
		final Method otherMethod) {
		// TODO: research method.getAnnotatedReturnType() and method.getGenericReturnType()
		return Objects.equals(method.getName(), otherMethod.getName())
			&& Objects.equals(method.getReturnType(), otherMethod.getReturnType())
			&& Arrays.equals(method.getParameterTypes(), otherMethod.getParameterTypes());
	}

}
