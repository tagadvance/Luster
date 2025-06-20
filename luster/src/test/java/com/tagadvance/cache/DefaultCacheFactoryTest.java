package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultCacheFactory}.
 */
class DefaultCacheFactoryTest {

	@Test
	void testException() {
		assertThrows(FooException.class,
			() -> new DefaultCacheFactory().newCache(ExpireAfterAccess.class,
				new ExpensiveOperationFailure()).proxy().expensiveOperation());

		assertThrows(FooException.class,
			() -> new DefaultCacheFactory().newCache(ExpireAfterWrite.class,
				new ExpensiveOperationFailure()).proxy().expensiveOperation());
	}

	@Test
	void testExpireAfterAccess() throws FooException, InterruptedException {
		final var controller = new DefaultCacheFactory().newCache(ExpireAfterAccess.class,
			new ExpensiveOperationSuccess());

		final ExpensiveOperation operation = controller.proxy();
		final var o1 = operation.expensiveOperation();
		assertNotNull(o1);
		final var o2 = operation.expensiveOperation();
		assertSame(o1, o2);

		// wait for expiration
		Thread.sleep(300);

		final var o3 = operation.expensiveOperation();
		assertNotSame(o1, o3);

		controller.getCache("ExpireAfterAccess").map(Cache::statistics).ifPresent(stats -> {
			assertEquals(1, stats.hitCount());
			assertEquals(2, stats.missCount());
			assertEquals(2, stats.loadSuccessCount());
			assertEquals(0, stats.loadExceptionCount());
			assertTrue(stats.averageLoadTime() > 0);
			assertTrue(stats.totalLoadTime() > 0);
			assertEquals(1, stats.evictionCount());
		});
	}

	@Test
	void testExpireAfterWrite() throws FooException, InterruptedException {
		final var controller = new DefaultCacheFactory().newCache(ExpireAfterWrite.class,
			new ExpensiveOperationSuccess());

		final ExpensiveOperation operation = controller.proxy();
		final var o1 = operation.expensiveOperation();
		assertNotNull(o1);
		final var o2 = operation.expensiveOperation();
		assertSame(o1, o2);

		// wait for expiration
		Thread.sleep(300);

		final var o3 = operation.expensiveOperation();
		assertNotSame(o1, o3);

		controller.getCache("ExpireAfterWrite").map(Cache::statistics).ifPresent(stats -> {
			assertEquals(1, stats.hitCount());
			assertEquals(2, stats.missCount());
			assertEquals(2, stats.loadSuccessCount());
			assertEquals(0, stats.loadExceptionCount());
			assertTrue(stats.averageLoadTime() > 0);
			assertTrue(stats.totalLoadTime() > 0);
			assertEquals(1, stats.evictionCount());
		});
	}

	@Test
	void testRefreshAfterWrite() throws FooException, InterruptedException {
		final var controller = new DefaultCacheFactory().newCache(RefreshAfterWrite.class,
			new ExpensiveOperationSuccess());

		final ExpensiveOperation operation = controller.proxy();
		final var o1 = operation.expensiveOperation();
		assertNotNull(o1);
		final var o2 = operation.expensiveOperation();
		assertSame(o1, o2);

		// expire and wait for refresh
		Thread.sleep(300);

		final var o3 = operation.expensiveOperation();
		assertNotSame(o1, o3);

		controller.getCache("RefreshAfterWrite").map(Cache::statistics).ifPresent(stats -> {
			assertEquals(2, stats.hitCount());
			assertEquals(1, stats.missCount());
			assertEquals(2, stats.loadSuccessCount());
			assertEquals(0, stats.loadExceptionCount());
			assertTrue(stats.averageLoadTime() > 0);
			assertTrue(stats.totalLoadTime() > 0);
			assertEquals(1, stats.evictionCount());
		});
	}

	@Test
	void testMaxSize() {
		final var controller = new DefaultCacheFactory().newCache(MaxSize.class,
			(Function<Integer, Object>) integer -> new Object());

		final MaxSize operation = controller.proxy();
		operation.apply(1);
		operation.apply(2);
		operation.apply(3);

		controller.getCache("MaxSize").map(Cache::size).ifPresent(size -> assertEquals(1, size));
	}

	@Test
	void testSoftValues() throws FooException {
		final var controller = new DefaultCacheFactory().newCache(SoftValues.class,
			new ExpensiveOperationSuccess());

		final ExpensiveOperation operation = controller.proxy();
		final var o1 = operation.expensiveOperation();
		assertNotNull(o1);
		final var o2 = operation.expensiveOperation();
		assertSame(o1, o2);
	}

	public interface ExpensiveOperation {

		Object expensiveOperation() throws FooException;

	}

	public static class ExpensiveOperationSuccess implements ExpensiveOperation {

		public Object expensiveOperation() {
			return new Object();
		}

	}

	public static class ExpensiveOperationFailure implements ExpensiveOperation {

		public Object expensiveOperation() throws FooException {
			throw new FooException();
		}

	}

	public interface ExpireAfterAccess extends ExpensiveOperation {

		@CacheConfiguration(name = "ExpireAfterAccess", expireAfterAccessDelay = 100L, recordStats = true)
		Object expensiveOperation() throws FooException;

	}

	public interface ExpireAfterWrite extends ExpensiveOperation {

		@CacheConfiguration(name = "ExpireAfterWrite", expireAfterWriteDelay = 100L, recordStats = true)
		Object expensiveOperation() throws FooException;

	}

	public interface RefreshAfterWrite extends ExpensiveOperation {

		@CacheConfiguration(name = "RefreshAfterWrite", expireAfterWriteDelay = 100L, refreshAfterWriteDelay = 100L, recordStats = true)
		Object expensiveOperation() throws FooException;

	}

	public interface MaxSize extends Function<Integer, Object> {

		@CacheConfiguration(name = "MaxSize", expireAfterWriteDelay = Long.MAX_VALUE, maximumSize = 1, recordStats = true)
		@Override
		Object apply(Integer i);

	}

	public interface SoftValues extends ExpensiveOperation {

		@CacheConfiguration(name = "SoftValues", expireAfterAccessDelay = 100L, softValues = true)
		Object expensiveOperation() throws FooException;

	}

	public static class FooException extends Exception {

	}

}
