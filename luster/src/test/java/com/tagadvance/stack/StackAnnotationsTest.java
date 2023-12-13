package com.tagadvance.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StackAnnotationsTest {

	@Test
	void testGetAnnotations() {
		new ExtendedFoo().foo(null);
	}

	@FunctionalInterface
	@DisplayName("interface Foo")
	private interface Foo {

		@Nullable
		void foo(Object o);

	}

	@DisplayName("class DefaultFoo")
	@PassDown("class DefaultFoo")
	private static class DefaultFoo implements Foo {

		@Override
		@DisplayName("DefaultFoo#foo(Object)")
		public void foo(final Object o) {
			final var annotations = new StackAnnotations(
				StackTraces.retain("com.tagadvance.*")).getAnnotations().toList();

			assertEquals(7, annotations.size());
			assertEquals(-1747740748, annotations.stream()
				.map(Object::toString)
				.collect(Collectors.joining(", "))
				.hashCode());
		}

	}

	@DisplayName("class ExtendedFoo")
	private static class ExtendedFoo extends DefaultFoo {

		@Override
		@DisplayName("ExtendedFoo#foo(Object)")
		public void foo(final Object o) {
			super.foo(o);
		}

	}

}
