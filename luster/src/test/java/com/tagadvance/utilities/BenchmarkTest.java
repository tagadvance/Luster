package com.tagadvance.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Supplier;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class BenchmarkTest {

	@Test
	void testRunnable() {
		final var runnable = mock(Runnable.class);
		Benchmark.wrap(runnable, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).run();
		verify(runnable).run();
	}

	@Test
	void testCallable() throws Exception {
		final var object = Benchmark.wrap((Callable<Object>) Object::new, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).call();
		assertNotNull(object, "object is null");
	}

	@Test
	void testSupplier() {
		final var object = Benchmark.wrap((Supplier<Object>) Object::new, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).get();
		assertNotNull(object, "object is null");
	}

	@Test
	void testPredicateSuccess() {
		var test = Benchmark.wrap(Objects::isNull, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).test(null);
		assertTrue(test);
	}

	@Test
	void testPredicateFailure() {
		var test = Benchmark.wrap(Objects::nonNull, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).test(new Object());
		assertTrue(test);
	}

	@Test
	void testFunctionSuccess() {
		final Object o = new Object();
		final var result = Benchmark.wrap((Function<Object, Boolean>) o::equals, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).apply(o);
		assertTrue(result);
	}

	@Test
	void testFunctionFailure() {
		final Object o = new Object();
		final var result = Benchmark.wrap((Function<Object, Boolean>) o::equals, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).apply(new Object());
		assertFalse(result);
	}

	@Test
	void testBiFunctionSuccess() {
		final Object o = new Object();
		final var result = Benchmark.wrap(Objects::equals, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).apply(o, o);
		assertTrue(result);
	}

	@Test
	void testBiFunctionFailure() {
		final var result = Benchmark.wrap(Objects::equals, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).apply(new Object(), new Object());
		assertFalse(result);
	}

	@Test
	void testConsumerSuccess() {
		final AtomicInteger i = new AtomicInteger();
		Benchmark.wrap((Consumer<AtomicInteger>) AtomicInteger::incrementAndGet, duration -> {
			assertNotNull(duration, "duration is null");
			assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
		}).accept(i);
		assertEquals(1, i.get());
	}

}
