package com.tagadvance.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link InvocationProxy}.
 */
class InvocationProxyTest {

	@Test
	public void testOnInvocation() {
		final AtomicInteger i = new AtomicInteger();
		InvocationProxy.createProxy(Runnable.class, i::getAndIncrement, invocation -> {
			assertEquals(0, i.get());

			return invocation.invoke();
		}).run();
	}

	@Test
	public void testRuntimeExceptionIsForwarded() {
		final String expectedMessage = "foo";
		final var exception = assertThrows(RuntimeException.class,
			() -> InvocationProxy.createProxy(Runnable.class, () -> {
				throw new RuntimeException(expectedMessage);
			}, Invocation::invoke).run());
		assertEquals(expectedMessage, exception.getMessage());
	}

}
