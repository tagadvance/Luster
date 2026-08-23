package com.tagadvance.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Checked}.
 */
class CheckedTest {

	private static final IOException CHECKED = new IOException("checked");

	@Test
	void rethrowingRestoresTheOriginalException() {
		final var thrown = assertThrows(IOException.class,
			() -> Checked.rethrowing(IOException.class, CheckedTest::throwsWrapped));

		assertSame(CHECKED, thrown);
	}

	@Test
	void rethrowingMakesAStreamPipelineHonest() {
		final var thrown = assertThrows(IOException.class,
			() -> Checked.rethrowing(IOException.class, CheckedTest::readAll));

		assertSame(CHECKED, thrown);
	}

	@Test
	void rethrowingLeavesNonMatchingCausesWrapped() {
		final var e = assertThrows(UncheckedException.class,
			() -> Checked.rethrowing(InterruptedException.class, CheckedTest::throwsWrapped));

		assertSame(CHECKED, e.getCause());
	}

	@Test
	void rethrowingPassesUnrelatedRuntimeExceptionsThrough() {
		final var unchecked = new IllegalStateException("unchecked");

		assertSame(unchecked, assertThrows(IllegalStateException.class,
			() -> Checked.rethrowing(IOException.class, () -> {
				throw unchecked;
			})));
	}

	@Test
	void rethrowingReturnsTheBodyResult() throws IOException {
		assertEquals("result", Checked.rethrowing(IOException.class, () -> "result"));
	}

	@Test
	void rethrowingRestoresTheOriginalExceptionFromASupplier() {
		final var thrown = assertThrows(IOException.class,
			() -> Checked.rethrowing(IOException.class, CheckedTest::supplyWrapped));

		assertSame(CHECKED, thrown);
	}

	private static void throwsWrapped() {
		throw new UncheckedException(CHECKED);
	}

	private static String supplyWrapped() {
		throw new UncheckedException(CHECKED);
	}

	private static void readAll() {
		Stream.of("a", "b")
			.map(CheckedFunction.of(CheckedTest::read))
			.forEach(s -> {
			});
	}

	private static String read(final String s) throws IOException {
		throw CHECKED;
	}

}
