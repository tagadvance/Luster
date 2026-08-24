package com.tagadvance.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link InvocationTest}.
 */
final class InvocationTest {

	@Test
	public void testInvoke() throws Throwable {
		final var method = getClass().getMethod("foo");

		new Invocation(null, method, null).invoke();
	}

	public static void foo() {
	}

	@Test
	public void testInvokeSetAccessible() throws Throwable {
		final var method = getClass().getDeclaredMethod("privateFoo");
		new Invocation(null, method, null).invoke();
	}

	private static void privateFoo() {
	}

	@Test
	public void testInvokeUnwrapsInvocationTargetException() throws Throwable {
		final var method = getClass().getMethod("fooThrowsException");
		final var invocation = new Invocation(null, method, null);

		final var thrown = assertThrows(Exception.class, invocation::invoke);

		assertEquals("foo", thrown.getMessage());
		assertEquals(Exception.class, thrown.getClass(), "the cause must not stay wrapped");
	}

	public static void fooThrowsException() throws Exception {
		throw new Exception("foo");
	}

	@Test
	public void testInvokeWithoutAnInstanceFailsClearly() throws Throwable {
		final var method = Runnable.class.getMethod("run");
		final var invocation = new Invocation(null, method, null);

		final var thrown = assertThrows(ProxyInvocationException.class, invocation::invoke);

		assertTrue(thrown.getMessage().contains("no instance to delegate to"), thrown.getMessage());
	}

	@Test
	public void testEqualityUsesArgumentValues() throws Throwable {
		final var method = getClass().getMethod("foo");

		final var one = new Invocation(null, method, null, "a", 1);
		final var two = new Invocation(null, method, null, "a", 1);

		assertEquals(one, two, "a record's generated equals would compare the array by identity");
		assertEquals(one.hashCode(), two.hashCode());
	}

	@Test
	public void testToStringRendersArguments() throws Throwable {
		final var method = getClass().getMethod("foo");

		final var invocation = new Invocation(null, method, null, "a", 1);

		assertEquals("InvocationTest#foo([a, 1])", invocation.toString());
	}

}
