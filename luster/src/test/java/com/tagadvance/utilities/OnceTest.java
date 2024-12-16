package com.tagadvance.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OnceTest {

	@Test
	void testSupplier() {
		final var count = new AtomicInteger();
		final var supplier = Once.supplier(count::incrementAndGet);
		IntStream.rangeClosed(0, 3).forEach(i -> {
			final var value = supplier.get();
			assertEquals(1, value);
		});
	}

	@Test
	void testRunnable() {
		final var count = new AtomicInteger();
		final var runnable = Once.runnable(count::incrementAndGet);
		IntStream.rangeClosed(0, 3).forEach(i -> {
			runnable.run();
			assertEquals(1, count.get());
		});
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

			assertEquals(1, count.get());
		});
	}

}
