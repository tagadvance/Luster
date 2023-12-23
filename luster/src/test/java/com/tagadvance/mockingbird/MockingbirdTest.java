package com.tagadvance.mockingbird;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tagadvance.stack.StackAnnotations;
import com.tagadvance.stack.StackTraces;
import org.junit.jupiter.api.Test;

public class MockingbirdTest {

	@Test
	@Mimic(path = "src/test/resources")
	void testMimic() {
		final var annotations = new StackAnnotations(StackTraces.retain("com.tagadvance.*"));
		final var mockingbird = new Mockingbird(annotations);
		final var proxy = mockingbird.createProxy(Api.class, new ApiImpl());

		final var result1 = proxy.call1();
		assertEquals("Lorem ipsum dolor sit amet,", result1);

		final var result2 = proxy.call2();
		assertEquals("consectetur adipiscing elit,", result2);
	}

	private interface Api {

		default String call1() {
			return "Lorem ipsum dolor sit amet,";
		}

		default String call2() {
			return "consectetur adipiscing elit,";
		}

	}

	private class ApiImpl implements Api {

	}

}
