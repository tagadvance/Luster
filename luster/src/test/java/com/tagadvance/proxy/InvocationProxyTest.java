package com.tagadvance.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link InvocationProxy}.
 */
class InvocationProxyTest {

	@Test
	public void testOnInvocation() {
		final AtomicInteger i = new AtomicInteger();
		InvocationProxy.createProxy(Runnable.class, i::getAndIncrement, invocation -> {
			assertEquals(0, i.get());

			return invocation.invoke();
		}).run();
	}

	@Test
	public void testRuntimeExceptionIsForwarded() {
		final String expectedMessage = "foo";
		final var exception = assertThrows(RuntimeException.class,
			() -> InvocationProxy.createProxy(Runnable.class, () -> {
				throw new RuntimeException(expectedMessage);
			}, Invocation::invoke).run());
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	void aDeclaredCheckedExceptionComesOutAsItself() {
		final var thrown = new IOException("boom");
		final Closeable proxy = InvocationProxy.createProxy(Closeable.class, () -> {
		}, invocation -> {
			throw thrown;
		});

		assertSame(thrown, assertThrows(IOException.class, proxy::close));
	}

	@Test
	void aDeclaredCheckedExceptionIsUnwrappedFromItsCause() {
		final var thrown = new IOException("boom");
		final Closeable proxy = InvocationProxy.createProxy(Closeable.class, () -> {
		}, invocation -> {
			throw new IllegalStateException("wrapper", thrown);
		});

		// the wrapper is unchecked, so it propagates as-is rather than being unwrapped
		final var actual = assertThrows(IllegalStateException.class, proxy::close);
		assertSame(thrown, actual.getCause());
	}

	@Test
	void anUndeclaredCheckedExceptionIsNamedRatherThanSwallowed() {
		final var thrown = new IOException("boom");
		final var proxy = InvocationProxy.createProxy(Api.class, () -> "real", invocation -> {
			throw thrown;
		});

		final var e = assertThrows(ProxyInvocationException.class, proxy::greeting);

		assertSame(thrown, e.getCause());
		assertTrue(e.getMessage().contains("does not declare"), e.getMessage());
		assertTrue(e.getMessage().contains("java.io.IOException"), e.getMessage());
	}

	@Test
	void anUndeclaredCheckedExceptionIsUnwrappedWhenItsCauseIsDeclared() {
		final var thrown = new IOException("boom");
		final Closeable proxy = InvocationProxy.createProxy(Closeable.class, () -> {
		}, invocation -> {
			throw new Exception("wrapper", thrown);
		});

		assertSame(thrown, assertThrows(IOException.class, proxy::close),
			"the declared cause should be found in the chain");
	}

	@Test
	void equalsIsIdentityAndSymmetric() {
		final var instance = new ApiImpl();
		final var proxy = InvocationProxy.createProxy(Api.class, instance, Invocation::invoke);

		assertEquals(proxy, proxy);
		assertFalse(proxy.equals(instance), "delegating equals would break symmetry");
		assertFalse(instance.equals(proxy));
	}

	@Test
	void twoProxiesOverOneInstanceAreNotEqual() {
		final var instance = new ApiImpl();

		assertNotEquals(InvocationProxy.createProxy(Api.class, instance, Invocation::invoke),
			InvocationProxy.createProxy(Api.class, instance, Invocation::invoke));
	}

	@Test
	void objectMethodsDoNotReachTheInterceptor() {
		final var reached = new AtomicInteger();
		final var proxy = InvocationProxy.createProxy(Api.class, new ApiImpl(), invocation -> {
			reached.incrementAndGet();

			return invocation.invoke();
		});

		proxy.hashCode();
		proxy.toString();
		proxy.equals(proxy);

		assertEquals(0, reached.get());
	}

	@Test
	void toStringIsDelegatedToTheInstance() {
		final var proxy = InvocationProxy.createProxy(Api.class, new ApiImpl(), Invocation::invoke);

		assertEquals("the real thing", proxy.toString());
	}

	@Test
	void aPureFakeNeedsNoInstance() {
		final var proxy = InvocationProxy.createProxy(Api.class, null,
			invocation -> "faked");

		assertEquals("faked", proxy.greeting());
		assertTrue(proxy.toString().startsWith("Api@"));
		assertEquals(proxy, proxy);
	}

	@Test
	void aPureFakeThatDelegatesFailsClearly() {
		final var proxy = InvocationProxy.createProxy(Api.class, null, Invocation::invoke);

		final var e = assertThrows(ProxyInvocationException.class, proxy::greeting);

		assertTrue(e.getMessage().contains("no instance to delegate to"), e.getMessage());
	}

	@Test
	void additionalInterfacesAreImplemented() {
		final var proxy = InvocationProxy.createProxy(Api.class, new ApiImpl(), invocation -> null,
			Closeable.class);

		assertTrue(proxy instanceof Closeable);
	}

	@Test
	void theProxyWorksWhenTheContextClassloaderIsUnrelated() throws Exception {
		final var thread = Thread.currentThread();
		final var original = thread.getContextClassLoader();
		try {
			// an empty loader that cannot see Api at all
			thread.setContextClassLoader(new java.net.URLClassLoader(new java.net.URL[0], null));

			final var proxy = InvocationProxy.createProxy(Api.class, new ApiImpl(),
				Invocation::invoke);

			assertEquals("the real thing", proxy.greeting());
		} finally {
			thread.setContextClassLoader(original);
		}
	}

	public interface Api {

		String greeting();

	}

	public static final class ApiImpl implements Api {

		@Override
		public String greeting() {
			return "the real thing";
		}

		@Override
		public String toString() {
			return "the real thing";
		}

	}

}
