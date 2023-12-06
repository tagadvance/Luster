package com.tagadvance.proxy;

import static java.util.Objects.requireNonNull;

import com.tagadvance.exception.UncheckedExecutionException;
import com.tagadvance.reflection.Mirror;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;

public class InvocationProxy {

	public static <I> I createProxy(final Class<I> iface, final I instance,
		final InvocationListener invocationListener) {
		requireNonNull(iface, "iface must not be null");

		final var classLoader = Thread.currentThread().getContextClassLoader();
		final var interfaces = new Class[]{iface};

		return (I) Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
			final var invocation = new Invocation(proxy, method, instance, args);

			try {
				return invocationListener.onInvocation(invocation);
			} catch (final Throwable t) {
				if (t instanceof RuntimeException) {
					throw t;
				}

				// Prevent UndeclaredThrowableException
				if (Stream.of(method)
					.flatMap(Mirror::getExceptionTypes)
					.anyMatch(exceptionType -> exceptionType.isInstance(t))) {
					throw t;
				}

				throw new UncheckedExecutionException(t);
			}
		});
	}

	private InvocationProxy() {
	}

	@FunctionalInterface
	public interface InvocationListener {

		Object onInvocation(final Invocation invocation) throws Throwable;

	}

}
