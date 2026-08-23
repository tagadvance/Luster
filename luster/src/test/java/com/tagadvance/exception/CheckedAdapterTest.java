package com.tagadvance.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Tests for the adapter family: {@link CheckedRunnable}, {@link CheckedComparator},
 * {@link CheckedConsumer}, {@link CheckedFunction}, and {@link CheckedPredicate}.
 */
class CheckedAdapterTest {

	private static final IOException CHECKED = new IOException("checked");

	private static final IllegalStateException UNCHECKED = new IllegalStateException("unchecked");

	@Test
	void functionWrapsCheckedException() {
		final CheckedFunction<String, String, IOException> function = i -> {
			throw CHECKED;
		};

		final var e = assertThrows(UncheckedException.class, () -> function.apply("i"));

		assertSame(CHECKED, e.getCause());
	}

	@Test
	void functionRethrowsUncheckedExceptionUnchanged() {
		final CheckedFunction<String, String, IOException> function = i -> {
			throw UNCHECKED;
		};

		assertSame(UNCHECKED, assertThrows(IllegalStateException.class, () -> function.apply("i")));
	}

	@Test
	void consumerWrapsCheckedException() {
		final CheckedConsumer<String, IOException> consumer = i -> {
			throw CHECKED;
		};

		final var e = assertThrows(UncheckedException.class, () -> consumer.accept("i"));

		assertSame(CHECKED, e.getCause());
	}

	@Test
	void consumerRethrowsUncheckedExceptionUnchanged() {
		final CheckedConsumer<String, IOException> consumer = i -> {
			throw UNCHECKED;
		};

		assertSame(UNCHECKED, assertThrows(IllegalStateException.class, () -> consumer.accept("i")));
	}

	@Test
	void predicateWrapsCheckedException() {
		final CheckedPredicate<String, IOException> predicate = i -> {
			throw CHECKED;
		};

		final var e = assertThrows(UncheckedException.class, () -> predicate.test("i"));

		assertSame(CHECKED, e.getCause());
	}

	@Test
	void predicateRethrowsUncheckedExceptionUnchanged() {
		final CheckedPredicate<String, IOException> predicate = i -> {
			throw UNCHECKED;
		};

		assertSame(UNCHECKED, assertThrows(IllegalStateException.class, () -> predicate.test("i")));
	}

	@Test
	void runnableWrapsCheckedException() {
		final CheckedRunnable<IOException> runnable = () -> {
			throw CHECKED;
		};

		final var e = assertThrows(UncheckedException.class, runnable::run);

		assertSame(CHECKED, e.getCause());
	}

	@Test
	void runnableRethrowsUncheckedExceptionUnchanged() {
		final CheckedRunnable<IOException> runnable = () -> {
			throw UNCHECKED;
		};

		assertSame(UNCHECKED, assertThrows(IllegalStateException.class, runnable::run));
	}

	@Test
	void comparatorWrapsCheckedException() {
		final CheckedComparator<String, IOException> comparator = (o1, o2) -> {
			throw CHECKED;
		};

		final var e = assertThrows(UncheckedException.class, () -> comparator.compare("a", "b"));

		assertSame(CHECKED, e.getCause());
	}

	@Test
	void comparatorRethrowsUncheckedExceptionUnchanged() {
		final CheckedComparator<String, IOException> comparator = (o1, o2) -> {
			throw UNCHECKED;
		};

		assertSame(UNCHECKED,
			assertThrows(IllegalStateException.class, () -> comparator.compare("a", "b")));
	}

	@Test
	void ofReturnsTheSameInstanceTypedAsTheJdkInterface() {
		final CheckedFunction<String, String, IOException> function = i -> i;
		final Function<String, String> of = CheckedFunction.of(function);

		assertSame(function, of);
		assertEquals("i", of.apply("i"));
	}

}
