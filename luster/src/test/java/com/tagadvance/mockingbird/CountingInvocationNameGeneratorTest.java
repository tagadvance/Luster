package com.tagadvance.mockingbird;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.tagadvance.mockingbird.CountingInvocationNameGenerator.MatchType;
import com.tagadvance.proxy.Invocation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CountingInvocationNameGenerator}.
 */
class CountingInvocationNameGeneratorTest {

	private final CountingInvocationNameGenerator generator = new CountingInvocationNameGenerator();

	@Test
	void theCounterIsPerDigest() throws Exception {
		final var invocation = invocationOf("bar", new Object[]{42, "test"}, int.class,
			String.class);

		final var name1 = generator.toName(IFoo.class, invocation);
		final var name2 = generator.toName(IFoo.class, invocation);

		assertEquals(name1.replace(".0000.", ".0001."), name2);
	}

	@Test
	void anUnrelatedInvocationDoesNotRenumber() throws Exception {
		final var bar = invocationOf("bar", new Object[]{42, "test"}, int.class, String.class);
		final var other = invocationOf("baz", new Object[]{}, new Class<?>[]{});

		final var first = generator.toName(IFoo.class, bar);
		generator.toName(IFoo.class, other);
		final var second = generator.toName(IFoo.class, bar);

		assertEquals(first.replace(".0000.", ".0001."), second,
			"an unrelated call must not renumber this one");
	}

	/**
	 * The bug that made recordings unfindable: {@code Arrays.hashCode} falls back to the identity
	 * hash for an argument that does not override {@code hashCode}, so two equal arguments hashed
	 * differently — within a single JVM, never mind across runs.
	 */
	@Test
	void equalArgumentsThatDoNotOverrideHashCodeProduceTheSameDigest() throws Exception {
		final var one = invocationOf("fetch", new Object[]{new Request("tenant-1")}, Request.class);
		final var two = invocationOf("fetch", new Object[]{new Request("tenant-1")}, Request.class);

		assertEquals(generator.digest(one), generator.digest(two));
	}

	@Test
	void differentArgumentsProduceDifferentDigests() throws Exception {
		final var one = invocationOf("fetch", new Object[]{new Request("tenant-1")}, Request.class);
		final var two = invocationOf("fetch", new Object[]{new Request("tenant-2")}, Request.class);

		assertNotEquals(generator.digest(one), generator.digest(two));
	}

	@Test
	void theDigestIsStableAcrossGeneratorInstances() throws Exception {
		final var invocation = invocationOf("fetch", new Object[]{new Request("tenant-1")},
			Request.class);

		assertEquals(new CountingInvocationNameGenerator().digest(invocation),
			new CountingInvocationNameGenerator().digest(invocation));
	}

	@Test
	void parameterTypeMatchingIgnoresArgumentValues() throws Exception {
		final var matching = new CountingInvocationNameGenerator(MatchType.PARAMETER_TYPE,
			new JsonMimicSerializer());
		final var one = invocationOf("fetch", new Object[]{new Request("tenant-1")}, Request.class);
		final var two = invocationOf("fetch", new Object[]{new Request("tenant-2")}, Request.class);

		assertEquals(matching.digest(one), matching.digest(two));
	}

	@Test
	void theGenericReturnTypeIsPartOfTheDigest() throws Exception {
		final var tenants = invocationOf("tenants", new Object[]{}, new Class<?>[]{});
		final var names = invocationOf("names", new Object[]{}, new Class<?>[]{});

		assertNotEquals(generator.digest(tenants), generator.digest(names));
	}

	private static Invocation invocationOf(final String name, final Object[] args,
		final Class<?>... parameterTypes) throws Exception {
		final var method = IFoo.class.getDeclaredMethod(name, parameterTypes);

		return new Invocation(null, method, new Foo(), args);
	}

	private interface IFoo {

		void bar(int i, String s);

		void baz();

		String fetch(Request request);

		List<String> tenants();

		List<Integer> names();

	}

	private static final class Foo implements IFoo {

		@Override
		public void bar(final int i, final String s) {
		}

		@Override
		public void baz() {
		}

		@Override
		public String fetch(final Request request) {
			return "";
		}

		@Override
		public List<String> tenants() {
			return List.of();
		}

		@Override
		public List<Integer> names() {
			return List.of();
		}

	}

	/**
	 * Deliberately overrides neither {@code equals} nor {@code hashCode}, like most request POJOs
	 * and vendor SDK types.
	 */
	private static final class Request {

		private final String tenant;

		private Request(final String tenant) {
			this.tenant = tenant;
		}

	}

}
