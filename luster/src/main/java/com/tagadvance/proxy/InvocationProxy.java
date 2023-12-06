package com.tagadvance.proxy;

import static java.util.Objects.requireNonNull;

import com.tagadvance.exception.UncheckedExecutionException;
import com.tagadvance.reflection.Mirror;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.stream.Stream;

public class InvocationProxy {

	/**
	 * @param iface             an interface
	 * @param instance          an instance of {@literal iface}
	 * @param invocationAdapter an {@link InvocationAdapter}
	 * @param <I>               the interface type
	 * @return a proxy
	 * @throws UncheckedExecutionException
	 */
	public static <I> I createProxy(final Class<I> iface, final I instance,
		final InvocationAdapter invocationAdapter) {
		requireNonNull(iface, "iface must not be null");

		final var classLoader = Thread.currentThread().getContextClassLoader();
		final var interfaces = new Class[]{iface};

		return (I) Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
			final var invocation = new Invocation(proxy, method, instance, args);

			try {
				return invocationAdapter.onInvocation(invocation);
			} catch (final InvocationTargetException e) {
				final var cause = e.getCause();

				throw propagateExpectedExceptions(method, cause);
			} catch (Throwable t) {
				throw propagateExpectedExceptions(method, t);
			}
		});
	}

	/**
	 * Prevent {@link UndeclaredThrowableException}.
	 */
	private static Throwable propagateExpectedExceptions(final Method method, final Throwable t) {
		if (t instanceof RuntimeException) {
			return t;
		} else if (Stream.of(method)
			.flatMap(Mirror::getExceptionTypes)
			.anyMatch(exceptionType -> exceptionType.isInstance(t))) {
			return t;
		}

		return new UncheckedExecutionException(t);
	}

	private InvocationProxy() {
	}

	@FunctionalInterface
	public interface InvocationAdapter {

		Object onInvocation(final Invocation invocation) throws Throwable;

	}

}
