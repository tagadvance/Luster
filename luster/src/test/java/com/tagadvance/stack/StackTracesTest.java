package com.tagadvance.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StackTracesTest {

	@Test
	void testAsStream() {
		final var list = StackTraces.asStream().toList();
		assertTrue(list.size() > 50);

		final var stackTraceElement = list.get(0);
		assertEquals("com.tagadvance.stack.StackTracesTest", stackTraceElement.getClassName());
		assertEquals("testAsStream", stackTraceElement.getMethodName());
	}

	@Test
	void testRetain() {
		final var retain = StackTraces.retain("com.tagadvance.*");
		final var list = StackTraces.asStream().filter(retain).toList();
		assertEquals(1, list.size());

		final var stackTraceElement = list.get(0);
		assertEquals("com.tagadvance.stack.StackTracesTest", stackTraceElement.getClassName());
		assertEquals("testRetain", stackTraceElement.getMethodName());
	}

}
