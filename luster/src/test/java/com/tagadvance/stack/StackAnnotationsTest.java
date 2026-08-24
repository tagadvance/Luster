package com.tagadvance.stack;

import static org.junit.jupiter.api.Assertions.*;

import com.tagadvance.stack.StackAnnotations.StackFrameAccessor;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class StackAnnotationsTest {

	private static final Pattern NAMESPACE = Pattern.compile("^com\\.tagadvance\\.stack\\.");

	@Test
	@DisplayName("annotations are collected from the caller upwards")
	void getAnnotations() {
		new ExtendedFoo().foo(null);
	}

	@Test
	@DisplayName("overloads are resolved by method type, not by name")
	void overloads() {
		bar("");
		bar(0);
	}

	@Test
	@DisplayName("a frame inside a lambda resolves to its declaring class and method")
	void lambdaFrame() {
		final Runnable runnable = () -> {
			final var accessor = new StackFrameAccessor(StackTraces.asStream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("expected at least one frame")));

			assertEquals(StackAnnotationsTest.class, accessor.getElementClass());

			final var method = accessor.findElementMethod()
				.orElseThrow(() -> new IllegalStateException("expected a synthetic lambda method"));
			assertTrue(method.isSynthetic());
			assertTrue(method.getName().startsWith("lambda$"));
		};

		runnable.run();
	}

	/**
	 * The frame belongs to a class the current class loader cannot see, so
	 * {@link Class#forName(String)} would resolve the wrong copy — or nothing at all.
	 */
	@Test
	@DisplayName("a frame from a foreign class loader resolves without a name lookup")
	void foreignClassLoader() throws Exception {
		final var loader = new IsolatedClassLoader();
		final var isolated = loader.loadClass(IsolatedCaller.class.getName());
		assertNotSame(IsolatedCaller.class, isolated);

		final var constructor = isolated.getDeclaredConstructor(Runnable.class);
		constructor.setAccessible(true);
		final var caller = (Runnable) constructor.newInstance((Runnable) () -> {
			final var accessor = StackTraces.asStream()
				.filter(frame -> "run".equals(frame.getMethodName()))
				.findFirst()
				.map(StackFrameAccessor::new)
				.orElseThrow(() -> new IllegalStateException("expected an IsolatedCaller frame"));

			assertSame(isolated, accessor.getElementClass());
			assertSame(loader, accessor.getElementClass().getClassLoader());
			assertEquals("run", accessor.getElementMethod().getName());
		});

		caller.run();
	}

	@DisplayName("bar(String)")
	private void bar(final String s) {
		assertBar(String.class, "bar(String)");
	}

	@DisplayName("bar(int)")
	private void bar(final int i) {
		assertBar(int.class, "bar(int)");
	}

	private static void assertBar(final Class<?> parameterType, final String displayName) {
		final var method = findBar().orElseThrow(
			() -> new IllegalStateException("expected a bar frame on the stack"));

		assertArrayEquals(new Class<?>[]{parameterType}, method.getParameterTypes());
		assertEquals(displayName, method.getAnnotation(DisplayName.class).value());
	}

	private static Optional<Method> findBar() {
		return StackTraces.asStream()
			.filter(frame -> "bar".equals(frame.getMethodName()))
			.findFirst()
			.map(StackFrameAccessor::new)
			.flatMap(StackFrameAccessor::findElementMethod);
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
				StackTraces.retain(NAMESPACE)).getAnnotations().toList();

			assertEquals(8, annotations.size(), annotations::toString);
			assertEquals("DefaultFoo#foo(Object)",
				assertInstanceOf(DisplayName.class, annotations.get(0)).value());
			assertEquals("class DefaultFoo",
				assertInstanceOf(DisplayName.class, annotations.get(1)).value());
			assertEquals("class DefaultFoo",
				assertInstanceOf(PassDown.class, annotations.get(2)).value());
			assertEquals("ExtendedFoo#foo(Object)",
				assertInstanceOf(DisplayName.class, annotations.get(3)).value());
			assertEquals("class ExtendedFoo",
				assertInstanceOf(DisplayName.class, annotations.get(4)).value());
			assertInstanceOf(Test.class, annotations.get(5));
			assertEquals("annotations are collected from the caller upwards",
				assertInstanceOf(DisplayName.class, annotations.get(6)).value());
			assertEquals("package com.tagadvance.stack",
				assertInstanceOf(PackageComment.class, annotations.get(7)).value());
		}

	}

	@DisplayName("class ExtendedFoo")
	private static final class ExtendedFoo extends DefaultFoo {

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

	/**
	 * A {@link ClassLoader class loader} with no parent but the bootstrap loader, so the classes it
	 * defines are invisible to {@link Class#forName(String)}.
	 */
	private static final class IsolatedClassLoader extends ClassLoader {

		private IsolatedClassLoader() {
			super(null);
		}

		@Override
		protected Class<?> findClass(final String name) throws ClassNotFoundException {
			final var resource = name.replace('.', '/') + ".class";
			try (final var in = StackAnnotationsTest.class.getClassLoader()
				.getResourceAsStream(resource)) {
				if (in == null) {
					throw new ClassNotFoundException(name);
				}

				final var bytes = in.readAllBytes();

				return defineClass(name, bytes, 0, bytes.length);
			} catch (final IOException e) {
				throw new ClassNotFoundException(name, e);
			}
		}

	}

}
