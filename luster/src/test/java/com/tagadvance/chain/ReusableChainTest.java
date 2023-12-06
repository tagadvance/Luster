package com.tagadvance.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ReusableChain}.
 */
public class ReusableChainTest {

	// TODO: test reusable

	@Test
	public void testFifoOrder() throws Throwable {
		final AtomicInteger i = new AtomicInteger();
		new ReusableChain<>(chain -> {
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
