package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * The behaviour the cache is expected to have, independent of how it stores entries. Written
 * against the hand-rolled implementation so that swapping the storage layer can be shown not to
 * change it.
 */
class CacheContractTest {

	@Test
	void concurrentCallsForOneKeyLoadOnce() throws Exception {
		final var loads = new AtomicInteger();
		final var controller = new DefaultCacheFactory().newCache(Slow.class, (Slow) () -> {
			loads.incrementAndGet();
			Thread.sleep(200);

			return new Object();
		});
		final Slow operation = controller.proxy();

		final var threads = 8;
		final var barrier = new CyclicBarrier(threads);
		final var executor = Executors.newFixedThreadPool(threads);
		try {
			final var futures = IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
				barrier.await();

				return operation.get();
			})).toList();

			final var first = futures.get(0).get(10, TimeUnit.SECONDS);
			for (final var future : futures) {
				assertSame(first, future.get(10, TimeUnit.SECONDS));
			}
		} finally {
			executor.shutdownNow();
		}

		assertEquals(1, loads.get(), "the value should have been loaded exactly once");
	}

	@Test
	void aCachedMethodMayCallItselfThroughTheProxy() {
		final var impl = new RecursiveImpl();
		final var controller = new DefaultCacheFactory().newCache(Recursive.class, impl);
		impl.proxy = controller.proxy();

		assertEquals(55, impl.proxy.fib(10));
	}

	@Test
	void nullReturnValuesAreCached() {
		final var loads = new AtomicInteger();
		final var controller = new DefaultCacheFactory().newCache(Nullable.class, (Nullable) () -> {
			loads.incrementAndGet();

			return null;
		});
		final Nullable operation = controller.proxy();

		assertNull(operation.get());
		assertNull(operation.get());

		assertEquals(1, loads.get(), "null should be cached like any other value");
	}

	@Test
	void aDeclaredCheckedExceptionPropagatesAsItself() {
		final var controller = new DefaultCacheFactory().newCache(Throwing.class, (Throwing) () -> {
			throw new FooException();
		});

		assertThrows(FooException.class, () -> controller.proxy().get());
	}

	@Test
	void aFailedLoadIsNotCached() {
		final var loads = new AtomicInteger();
		final var controller = new DefaultCacheFactory().newCache(Throwing.class, (Throwing) () -> {
			loads.incrementAndGet();

			throw new FooException();
		});
		final Throwing operation = controller.proxy();

		assertThrows(FooException.class, operation::get);
		assertThrows(FooException.class, operation::get);

		assertEquals(2, loads.get(), "a failed load must not be cached");
	}

	@Test
	void aTypedReturnValueComesBackAsItself() {
		final var controller = new DefaultCacheFactory().newCache(Named.class,
			(Named) () -> "tenant");

		assertEquals("tenant", controller.proxy().name());
	}

	@Test
	void anInvalidatedEntryIsLoadedAgain() {
		final var loads = new AtomicInteger();
		final var controller = new DefaultCacheFactory().newCache(Named.class, (Named) () -> {
			loads.incrementAndGet();

			return "tenant";
		});

		controller.proxy().name();
		controller.proxy().name();
		assertEquals(1, loads.get());

		controller.getCache("Named").orElseThrow().invalidate();

		controller.proxy().name();
		assertEquals(2, loads.get());
	}

	@Test
	void cachesExistBeforeTheFirstInvocation() {
		final var controller = new DefaultCacheFactory().newCache(Named.class,
			(Named) () -> "tenant");

		assertEquals(1, controller.caches().size());
		assertEquals("Named", controller.getCache("Named").orElseThrow().name());
	}

	@Test
	void distinctArgumentsGetDistinctEntries() {
		final var controller = new DefaultCacheFactory().newCache(Keyed.class,
			(Keyed) i -> new Object());
		final Keyed operation = controller.proxy();

		final var one = operation.apply(1);
		final var two = operation.apply(2);

		assertSame(one, operation.apply(1));
		assertSame(two, operation.apply(2));
	}

	public interface Slow {

		@CacheConfiguration(name = "Slow")
		Object get() throws Exception;

	}

	public interface Nullable {

		@CacheConfiguration(name = "Nullable")
		Object get();

	}

	public interface Throwing {

		@CacheConfiguration(name = "Throwing")
		Object get() throws FooException;

	}

	public interface Keyed {

		@CacheConfiguration(name = "Keyed")
		Object apply(int i);

	}

	public interface Named {

		@CacheConfiguration(name = "Named")
		String name();

	}

	public interface Recursive {

		@CacheConfiguration(name = "Recursive")
		int fib(int n);

	}

	public static class RecursiveImpl implements Recursive {

		Recursive proxy;

		@Override
		public int fib(final int n) {
			return n < 2 ? n : proxy.fib(n - 1) + proxy.fib(n - 2);
		}

	}

	@SuppressWarnings("serial")
	public static class FooException extends Exception {

	}

}
