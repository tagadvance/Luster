package com.tagadvance.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OnError}.
 */
class OnErrorTest {

	private static final IOException CHECKED = new IOException("checked");

	@Test
	void handlerIsInvokedOncePerFailure() {
		final var count = new AtomicInteger();
		final var onError = OnError.of(e -> count.incrementAndGet());

		Stream.of("a", "b", "c").forEach(onError.consumer(OnErrorTest::throwingConsumer));

		assertEquals(3, count.get());
	}

	@Test
	void handlerReceivesTheOriginalException() {
		final List<Exception> handled = new ArrayList<>();

		OnError.of(handled::add).runnable(OnErrorTest::throwingRunnable).run();

		assertEquals(1, handled.size());
		assertSame(CHECKED, handled.get(0));
	}

	@Test
	void theStreamContinuesPastAFailure() {
		final var onError = OnError.of(e -> {
		});

		final var results = Stream.of("a", "fail", "c")
			.map(onError.optionalFunction(OnErrorTest::failOnFail))
			.flatMap(Optional::stream)
			.toList();

		assertEquals(List.of("a", "c"), results);
	}

	@Test
	void optionalFunctionYieldsEmptyOnFailure() {
		final var function = OnError.of(e -> {
		}).optionalFunction(OnErrorTest::throwingFunction);

		assertTrue(function.apply("a").isEmpty());
	}

	@Test
	void optionalSupplierYieldsEmptyOnFailure() {
		final var supplier = OnError.of(e -> {
		}).optionalSupplier(OnErrorTest::throwingSupplier);

		assertTrue(supplier.get().isEmpty());
	}

	@Test
	void defaultValueIsUsedOnFailure() {
		final var function = OnError.of(e -> {
		}).function(OnErrorTest::throwingFunction, () -> "default");

		assertEquals("default", function.apply("a"));
	}

	@Test
	void predicateDefaultsToFalse() {
		final var predicate = OnError.of(e -> {
		}).predicate(OnErrorTest::throwingPredicate);

		assertEquals(List.of(), Stream.of("a", "b").filter(predicate).toList());
	}

	@Test
	void predicateHonorsAnExplicitDefault() {
		final var predicate = OnError.of(e -> {
		}).predicate(OnErrorTest::throwingPredicate, () -> true);

		assertEquals(List.of("a", "b"), Stream.of("a", "b").filter(predicate).toList());
	}

	@Test
	void uncheckedExceptionsAreHandledToo() {
		final List<Exception> handled = new ArrayList<>();
		final var unchecked = new IllegalStateException("unchecked");

		OnError.of(handled::add).runnable(() -> {
			throw unchecked;
		}).run();

		assertEquals(1, handled.size());
		assertSame(unchecked, handled.get(0));
	}

	@Test
	void nothingIsHandledWhenNothingFails() {
		final var onError = OnError.of(Assertions::fail);

		final var results = Stream.of("a", "b")
			.map(onError.optionalFunction(s -> s))
			.flatMap(Optional::stream)
			.toList();

		assertEquals(List.of("a", "b"), results);
	}

	private static String failOnFail(final String s) throws IOException {
		if ("fail".equals(s)) {
			throw CHECKED;
		}

		return s;
	}

	private static void throwingConsumer(final String s) throws IOException {
		throw CHECKED;
	}

	private static void throwingRunnable() throws IOException {
		throw CHECKED;
	}

	private static String throwingFunction(final String s) throws IOException {
		throw CHECKED;
	}

	private static String throwingSupplier() throws IOException {
		throw CHECKED;
	}

	private static boolean throwingPredicate(final String s) throws IOException {
		throw CHECKED;
	}

}
