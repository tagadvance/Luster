package com.tagadvance.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

			assertEquals(5, annotations.size());
			{
				final var value = assertInstanceOf(DisplayName.class, annotations.get(0)).value();
				assertEquals("ExtendedFoo#foo(Object)", value);
			}
			{
				final var value = assertInstanceOf(DisplayName.class, annotations.get(1)).value();
				assertEquals("class ExtendedFoo", value);
			}
			{
				final var value = assertInstanceOf(PassDown.class, annotations.get(2)).value();
				assertEquals("class DefaultFoo", value);
			}
			assertInstanceOf(Test.class, annotations.get(3));
			{
				final var value = assertInstanceOf(PackageComment.class,
					annotations.get(4)).value();
				assertEquals("package com.tagadvance.stack", value);
			}
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

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Nullable {

	}

}
