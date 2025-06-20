package com.tagadvance.proxy;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Proxy;

/**
 * Create {@link Proxy proxies} with an {@link InvocationInterceptor invocation callback}.
 */
public class InvocationProxy {

	/**
	 * @param iface       an interface
	 * @param instance    an instance of {@literal iface}
	 * @param interceptor an {@link InvocationInterceptor}
	 * @param <T>         the instance type
	 * @param <I>         the interface type
	 * @return a proxy
	 */
	@SuppressWarnings("unchecked")
	public static <T, I extends T> I createProxy(final Class<I> iface, final T instance,
		final InvocationInterceptor interceptor) {
		requireNonNull(iface, "iface must not be null");
		requireNonNull(instance, "instance must not be null");
		requireNonNull(interceptor, "interceptor must not be null");

		final var classLoader = Thread.currentThread().getContextClassLoader();
		final var interfaces = new Class[]{iface};

		return (I) Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
			final var invocation = new Invocation(proxy, method, instance, args);

			return interceptor.onInvocation(invocation);
		});
	}

	private InvocationProxy() {
	}

}
