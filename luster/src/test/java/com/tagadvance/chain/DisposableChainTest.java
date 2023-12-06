package com.tagadvance.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tagadvance.proxy.InvocationProxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DisposableChain}.
 */
public class DisposableChainTest {

	// TODO: test only works once

	@Test
	public void testFifoOrder() throws Throwable {
		final AtomicInteger i = new AtomicInteger();
		new DisposableChain(chain -> {
			assertEquals(i.getAndIncrement(), 0);

			final var input = chain.input();

			return chain.next(input);
		}, chain -> {
			assertEquals(i.getAndIncrement(), 1);

			final var input = chain.input();

			return chain.next(input);
		}).next(() -> null);
	}

}