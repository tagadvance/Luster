package com.tagadvance.proxy;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link InvocationTest}.
 */
public class InvocationTest {

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

		assertThrows(Exception.class, invocation::invoke);
	}

	public static void fooThrowsException() throws Exception {
		throw new Exception("foo");
	}

}
