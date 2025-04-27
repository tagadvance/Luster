package com.tagadvance.mockingbird;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class MockingbirdTest {

	@Test
	void testMimic() {
		final var mockingbird = new Mockingbird(Path.of("src/test/resources"));
		final Api proxy = mockingbird.createProxy(Api.class, new ApiImpl());

		final var result1 = proxy.call1();
		assertEquals("Lorem ipsum dolor sit amet,", result1);

		final var result2 = proxy.call2();
		assertEquals("consectetur adipiscing elit,", result2);
	}

	private interface Api {

		boolean IS_RECORDING = false;

		default String call1() {
			return IS_RECORDING ? "Lorem ipsum dolor sit amet," : "...";
		}

		default String call2() {
			return IS_RECORDING ? "consectetur adipiscing elit," : "...";
		}

	}

	private static class ApiImpl implements Api {

	}

}
