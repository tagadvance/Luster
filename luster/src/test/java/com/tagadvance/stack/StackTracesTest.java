package com.tagadvance.stack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StackTracesTest {

	private static final Pattern NAMESPACE = Pattern.compile("^com\\.tagadvance\\.");

	@Test
	@DisplayName("asStream() starts at the caller and drops JDK frames")
	void asStream() {
		final var list = StackTraces.asStream().toList();
		assertTrue(list.size() > 10, "expected a deep stack, got " + list.size());

		final var frame = list.get(0);
		assertEquals(StackTracesTest.class, frame.getDeclaringClass());
		assertEquals("asStream", frame.getMethodName());

		assertTrue(list.stream()
			.noneMatch(f -> StackTraces.JDK_PACKAGES.matcher(f.getClassName()).find()));
	}

	@Test
	@DisplayName("walk() reports the caller as the first frame")
	void walk() {
		final var frame = StackTraces.walk(Stream::findFirst)
			.orElseThrow(() -> new IllegalStateException("a walk always has at least one frame"));

		assertEquals(StackTracesTest.class, frame.getDeclaringClass());
		assertEquals("walk", frame.getMethodName());
	}

	@Test
	@DisplayName("retain(Class) keeps only the frames that class declares")
	void retainClass() {
		final var list = StackTraces.asStream()
			.filter(StackTraces.retain(StackTracesTest.class))
			.toList();

		assertEquals(1, list.size());
		assertEquals("retainClass", list.get(0).getMethodName());
	}

	@Test
	@DisplayName("remove(Class) drops every frame that class declares")
	void removeClass() {
		final var list = StackTraces.asStream()
			.filter(StackTraces.remove(StackTracesTest.class))
			.toList();

		assertFalse(list.isEmpty());
		assertTrue(list.stream().noneMatch(StackTraces.retain(StackTracesTest.class)));
	}

	@Test
	@DisplayName("retain(Pattern) is unanchored")
	void retainUnanchored() {
		final var element = new StackTraceElement("org.evil.com.tagadvance.Thing", "run", null, 1);

		assertTrue(StackTraces.retainElement(Pattern.compile("com\\.tagadvance")).test(element));
		assertFalse(StackTraces.retainElement(NAMESPACE).test(element));
	}

	@Test
	@DisplayName("JDK_PACKAGES matches JDK packages and only those")
	void jdkPackages() {
		Stream.of("java.lang.Thread", "javax.naming.Context", "jdk.internal.misc.Unsafe",
				"sun.nio.ch.IOUtil", "com.sun.crypto.provider.AESCipher", "com.oracle.net.Sdp",
				"jrt.Foo", "org.w3c.dom.Node", "org.xml.sax.Parser")
			.forEach(name -> assertTrue(isJdkPackage(name), name));

		Stream.of("javassist.Foo", "jdkfoo.Bar", "sunny.Day", "com.tagadvance.stack.StackTraces")
			.forEach(name -> assertFalse(isJdkPackage(name), name));
	}

	@Test
	@DisplayName("prune(Throwable, Pattern) leaves the throwable untouched")
	void prune() {
		final var throwable = new RuntimeException("boom");
		final var before = throwable.getStackTrace();

		final var pruned = StackTraces.prune(throwable, NAMESPACE);

		assertTrue(pruned.length > 0);
		assertTrue(pruned.length < before.length, "nothing was pruned");
		assertArrayEquals(before, throwable.getStackTrace());
	}

	@Test
	@DisplayName("pruneInPlace(Throwable, Pattern) prunes the whole causal chain")
	void pruneInPlace() {
		final var cause = new IllegalStateException("cause");
		final var throwable = new RuntimeException("boom", cause);

		StackTraces.pruneInPlace(throwable, NAMESPACE);

		Stream.of(throwable, cause).forEach(t -> {
			final var stackTrace = t.getStackTrace();
			assertTrue(stackTrace.length > 0);
			assertTrue(Stream.of(stackTrace).allMatch(StackTraces.retainElement(NAMESPACE)));
		});
	}

	@Test
	@DisplayName("isTesting() is true under JUnit")
	void isTesting() {
		assertTrue(StackTraces.isTesting());
	}

	private static boolean isJdkPackage(final String className) {
		return StackTraces.JDK_PACKAGES.matcher(className).find();
	}

}
