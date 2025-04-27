package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tagadvance.reflection.ReflectionException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CacheUtils}.
 */
class CacheUtilsTest {

	@Test
	void testToValidExceptionPrioritizesMatchingMethodSignature() throws Exception {
		final var fooException = new FooException();
		final var reflectionException = new ReflectionException(fooException);
		final var method = Foo1.class.getDeclaredMethod("bar");
		var validException = CacheUtils.toValidException(reflectionException, method);

		assertEquals(fooException, validException);
	}

	@Test
	void testToValidExceptionPrioritizesRuntimeException() throws Exception {
		final var fooException = new FooException();
		final var runtimeException = new RuntimeException(fooException);
		final var reflectionException = new ReflectionException(runtimeException);
		final var method = Foo1.class.getDeclaredMethod("bar");
		var validException = CacheUtils.toValidException(reflectionException, method);

		assertEquals(runtimeException, validException);
	}

	@Test
	void testToValidExceptionDefaultsToReflectionException() throws Exception {
		final var e = new Exception();
		final var reflectionException = new ReflectionException(e);
		final var method = Foo1.class.getDeclaredMethod("bar");
		var validException = CacheUtils.toValidException(reflectionException, method);

		assertEquals(reflectionException, validException);
	}

	@Test
	void testMethodHashCode() throws Exception {
		final var method = Foo1.class.getDeclaredMethod("bar");
		final var hashCode = CacheUtils.methodHashCode(method);

		assertEquals(-1558268459, hashCode);
	}

	@Test
	void testMethodSignatureEquals() throws Exception {
		final var foo1bar = Foo1.class.getDeclaredMethod("bar");
		final var foo2bar = Foo1.class.getDeclaredMethod("bar");
		assertTrue(CacheUtils.methodSignatureEquals(foo1bar, foo2bar));

		final var foo1bar1 = Foo1.class.getDeclaredMethod("bar1", Object.class, int.class);
		final var foo2bar1 = Foo1.class.getDeclaredMethod("bar1", Object.class, int.class);
		assertTrue(CacheUtils.methodSignatureEquals(foo1bar1, foo2bar1));
	}

	final class Foo1 {

		void bar() throws FooException {
		}

		Object bar1(Object o, int i) {
			return null;
		}

	}

	final class Foo2 {

		void bar() throws FooException {
		}

		Object bar1(Object o, int i) {
			return null;
		}

	}

	static final class FooException extends Exception {

		public FooException() {
			super();
		}

		public FooException(final String message) {
			super(message);
		}

		public FooException(final String message, final Throwable cause) {
			super(message, cause);
		}

		public FooException(final Throwable cause) {
			super(cause);
		}

	}

}
