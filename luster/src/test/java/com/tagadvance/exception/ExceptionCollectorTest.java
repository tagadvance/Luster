package com.tagadvance.exception;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExceptionCollector}.
 */
class ExceptionCollectorTest {

	@Test
	void closeDoesNotThrowWhenNothingFailed() {
		assertDoesNotThrow(() -> {
			try (final var collector = ExceptionCollector.create()) {
				Stream.of("a", "b").forEach(collector.consumer(s -> {
				}));
			}
		});
	}

	@Test
	void closeThrowsWithTheRemainingFailuresSuppressed() {
		final var e = assertThrows(UncheckedException.class, () -> {
			try (final var collector = ExceptionCollector.create()) {
				Stream.of("a", "b", "c").forEach(collector.consumer(ExceptionCollectorTest::sync));
			}
		});

		assertEquals("a", e.getCause().getMessage());
		assertEquals(2, e.getSuppressed().length);
		assertEquals("b", e.getSuppressed()[0].getMessage());
		assertEquals("c", e.getSuppressed()[1].getMessage());
	}

	@Test
	void everyElementIsVisitedDespiteFailures() {
		final var collector = ExceptionCollector.create();

		Stream.of("a", "b", "c").forEach(collector.consumer(ExceptionCollectorTest::sync));

		assertEquals(List.of("a", "b", "c"),
			collector.getExceptions().stream().map(Exception::getMessage).toList());
	}

	@Test
	void theOriginalTypeIsRecoverableAtTheBoundary() {
		final var thrown = assertThrows(IOException.class,
			() -> Checked.rethrowing(IOException.class, ExceptionCollectorTest::syncAll));

		assertEquals("a", thrown.getMessage());
	}

	private static void syncAll() {
		try (final var collector = ExceptionCollector.create()) {
			Stream.of("a", "b").forEach(collector.consumer(ExceptionCollectorTest::sync));
		}
	}

	private static void sync(final String s) throws IOException {
		throw new IOException(s);
	}

	@Test
	void getExceptionsIsASnapshot() {
		final var collector = ExceptionCollector.create();
		collector.handleException(new IOException("a"));
		final var snapshot = collector.getExceptions();
		collector.handleException(new IOException("b"));

		assertEquals(1, snapshot.size());
		assertEquals(2, collector.getExceptions().size());
		assertSame(snapshot.get(0), collector.getExceptions().get(0));
	}

}
