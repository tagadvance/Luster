package com.tagadvance.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link InterceptorProxy}.
 */
public class InterceptorProxyTest {

	@Test
	public void testFifoOrder() {
		final AtomicInteger i = new AtomicInteger();
		new InterceptorProxy<>(Runnable.class, () -> {
		}).withInterceptors(chain -> {
			assertEquals(i.getAndIncrement(), 0);

			final var invocation = chain.invocation();

			return chain.proceed(invocation);
		}, chain -> {
			assertEquals(i.getAndIncrement(), 1);

			final var invocation = chain.invocation();

			return chain.proceed(invocation);
		}).getProxy().run();
	}

	@Test
	public void testLifoOrder() {
		final AtomicInteger i = new AtomicInteger();
		new InterceptorProxy<>(Runnable.class, () -> {
		}).withInterceptor(chain -> {
			assertEquals(i.getAndIncrement(), 1);

			final var invocation = chain.invocation();

			return chain.proceed(invocation);
		}).withInterceptor(chain -> {
			assertEquals(i.getAndIncrement(), 0);

			final var invocation = chain.invocation();

			return chain.proceed(invocation);
		}).getProxy().run();
	}

}
