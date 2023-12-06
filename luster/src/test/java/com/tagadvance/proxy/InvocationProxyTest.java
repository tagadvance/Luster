package com.tagadvance.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tagadvance.chain.DisposableChain;
import com.tagadvance.exception.UncheckedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DisposableChain}.
 */
public class InvocationProxyTest {

	@Test
	public void onInvocation() {
		final AtomicInteger i = new AtomicInteger();
		InvocationProxy.createProxy(Runnable.class, i::getAndIncrement, invocation -> {
			assertEquals(0, i.get());

			return invocation.call();
		}).run();
	}

	@Test
	public void testRuntimeExceptionIsForwarded() {
		final String expectedMessage = "foo";
		final var exception = assertThrows(RuntimeException.class,
			() -> InvocationProxy.createProxy(Runnable.class, () -> {
				throw new RuntimeException(expectedMessage);
			}, Invocation::call).run());
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	public void testUndeclaredThrowableExceptionIsNotThrown() {
		assertThrows(UncheckedExecutionException.class,
			() -> InvocationProxy.createProxy(Runnable.class, () -> {
			}, invocation -> {
				// force an IllegalAccessException
				InvocationProxy.class.getDeclaredConstructor().newInstance();

				return null;
			}).run());
	}

}
