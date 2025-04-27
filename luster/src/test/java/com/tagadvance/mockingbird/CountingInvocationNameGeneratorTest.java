package com.tagadvance.mockingbird;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tagadvance.proxy.Invocation;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CountingInvocationNameGenerator}.
 */
class CountingInvocationNameGeneratorTest {

	@Test
	public void testToName() throws Exception {
		final var method = Foo.class.getDeclaredMethod("bar", int.class, String.class);
		final var invocation = new Invocation(null, method, new Foo(), 42, "test");

		final var generator = new CountingInvocationNameGenerator();

		final var name1 = generator.toName(IFoo.class, invocation);
		assertEquals("IFoo.d00e04a9.0000.gson", name1);

		final var name2 = generator.toName(IFoo.class, invocation);
		assertEquals("IFoo.d00e04a9.0001.gson", name2);
	}

	private interface IFoo {

		void bar(int i, String s);

	}

	private static final class Foo implements IFoo {

		public void bar(int i, String s) {

		}

	}

}
