package com.tagadvance.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OnceTest {

	private static final int THREADS = 8;

	@Test
	void testSupplier() {
		final var count = new AtomicInteger();
		final var supplier = Once.supplier(count::incrementAndGet);
		IntStream.rangeClosed(0, 3).forEach(i -> supplier.get());

		assertEquals(1, count.get());
	}

	@Test
	void testSupplierWithNullValue() {
		final var count = new AtomicInteger();
		final var supplier = Once.supplier(() -> {
			count.incrementAndGet();

			return null;
		});
		IntStream.rangeClosed(0, 3).forEach(i -> supplier.get());

		assertEquals(1, count.get());
	}

	@Test
	void testRunnable() {
		final var count = new AtomicInteger();
		final var runnable = Once.runnable(count::incrementAndGet);
		IntStream.rangeClosed(0, 3).forEach(i -> runnable.run());

		assertEquals(1, count.get());
	}

	@Test
	void testCallable() {
		final var count = new AtomicInteger();
		final var runnable = Once.callable(count::incrementAndGet);
		IntStream.rangeClosed(0, 3).forEach(i -> {
			try {
				runnable.call();
			} catch (final Exception e) {
				fail(e);
			}
		});

		assertEquals(1, count.get());
	}

	@Test
	void testCallableWithNullValue() {
		final var count = new AtomicInteger();
		final var runnable = Once.callable(() -> {
			count.incrementAndGet();

			return null;
		});
		IntStream.rangeClosed(0, 3).forEach(i -> {
			try {
				runnable.call();
			} catch (final Exception e) {
				fail(e);
			}
		});

		assertEquals(1, count.get());
	}

	@Test
	@DisplayName("a contended supplier is invoked exactly once")
	void supplierContended() throws Exception {
		final var count = new AtomicInteger();
		final var supplier = Once.supplier(count::incrementAndGet);
		final var results = race(supplier::get);

		assertEquals(1, count.get());
		assertEquals(List.of(1, 1, 1, 1, 1, 1, 1, 1), results);
	}

	@Test
	@DisplayName("a contended callable is invoked exactly once")
	void callableContended() throws Exception {
		final var count = new AtomicInteger();
		final var callable = Once.callable(count::incrementAndGet);
		final var results = race(callable);

		assertEquals(1, count.get());
		assertEquals(List.of(1, 1, 1, 1, 1, 1, 1, 1), results);
	}

	@Test
	@DisplayName("a callable that fails is retried by the next caller")
	void callableFailureIsNotCached() throws Exception {
		final var count = new AtomicInteger();
		final var callable = Once.callable(() -> {
			if (count.incrementAndGet() == 1) {
				throw new IllegalStateException("boom");
			}

			return count.get();
		});

		assertThrows(IllegalStateException.class, callable::call);
		assertEquals(2, callable.call());
		assertEquals(2, callable.call());
		assertEquals(2, count.get());
	}

	/**
	 * Invoke the supplied task on {@value #THREADS} threads released simultaneously by a
	 * {@link CyclicBarrier}.
	 */
	private static <T> List<T> race(final Callable<T> task) throws Exception {
		final var barrier = new CyclicBarrier(THREADS);
		final var service = Executors.newFixedThreadPool(THREADS);
		try {
			final var futures = IntStream.range(0, THREADS).mapToObj(i -> service.submit(() -> {
				barrier.await();

				return task.call();
			})).toList();

			final var results = new ArrayList<T>();
			for (final var future : futures) {
				results.add(future.get(10, TimeUnit.SECONDS));
			}

			return results;
		} finally {
			service.shutdownNow();
		}
	}

}
