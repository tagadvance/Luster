package com.tagadvance.utilities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimedTest {

	private static void assertValid(final Duration duration) {
		assertNotNull(duration, "duration is null");
		assertTrue(!duration.isZero() && !duration.isNegative(), "invalid duration");
	}

	@Test
	@DisplayName("a bare lambda resolves without an ambiguity error")
	void bareLambda() {
		final var value = Timed.profile(() -> "value", TimedTest::assertValid);

		assertEquals("value", value);
	}

	@Test
	@DisplayName("the duration is reported even when the operation throws")
	void profileThrows() {
		final var reported = new AtomicReference<Duration>();
		final var expected = new IllegalStateException("boom");

		final var actual = assertThrows(IllegalStateException.class,
			() -> Timed.profile(() -> {
				throw expected;
			}, reported::set));

		assertSame(expected, actual);
		assertValid(reported.get());
	}

	@Test
	@DisplayName("a checked exception propagates unwrapped")
	void profileChecked() {
		final var expected = new Exception("boom");

		final var actual = assertThrows(Exception.class, () -> Timed.profile(() -> {
			throw expected;
		}, TimedTest::assertValid));

		assertSame(expected, actual);
	}

	@Test
	void testRunnable() {
		final var runnable = mock(Runnable.class);
		Timed.wrap(runnable, TimedTest::assertValid).run();
		verify(runnable).run();
	}

	@Test
	void testCallable() throws Exception {
		final var object = Timed.wrap((Callable<Object>) Object::new, TimedTest::assertValid)
			.call();
		assertNotNull(object, "object is null");
	}

	@Test
	void testSupplier() {
		final var object = Timed.wrap((Supplier<Object>) Object::new, TimedTest::assertValid).get();
		assertNotNull(object, "object is null");
	}

	@Test
	@DisplayName("a predicate is timed through the function overload")
	void testPredicate() {
		final Predicate<Object> isNull = Timed.wrap((Function<Object, Boolean>) Objects::isNull,
			TimedTest::assertValid)::apply;

		assertTrue(isNull.test(null));
		assertFalse(isNull.test(new Object()));
	}

	@Test
	void testFunctionSuccess() {
		final Object o = new Object();
		final var result = Timed.wrap((Function<Object, Boolean>) o::equals,
			TimedTest::assertValid).apply(o);
		assertTrue(result);
	}

	@Test
	void testFunctionFailure() {
		final Object o = new Object();
		final var result = Timed.wrap((Function<Object, Boolean>) o::equals,
			TimedTest::assertValid).apply(new Object());
		assertFalse(result);
	}

	@Test
	void testBiFunctionSuccess() {
		final Object o = new Object();
		final var result = Timed.wrap(Objects::equals, TimedTest::assertValid).apply(o, o);
		assertTrue(result);
	}

	@Test
	void testBiFunctionFailure() {
		final var result = Timed.wrap(Objects::equals, TimedTest::assertValid)
			.apply(new Object(), new Object());
		assertFalse(result);
	}

	@Test
	void testConsumerSuccess() {
		final var i = new AtomicInteger();
		Timed.wrap((Consumer<AtomicInteger>) AtomicInteger::incrementAndGet,
			TimedTest::assertValid).accept(i);
		assertEquals(1, i.get());
	}

}
