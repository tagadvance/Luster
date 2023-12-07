package com.tagadvance.proxy;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Proxy;

public class InvocationProxy {

	/**
	 * @param iface              an interface
	 * @param instance           an instance of {@literal iface}
	 * @param invocationCallback an {@link InvocationCallback}
	 * @param <I>                the interface type
	 * @return a proxy
	 */
	public static <I> I createProxy(final Class<I> iface, final I instance,
		final InvocationCallback invocationCallback) {
		requireNonNull(iface, "iface must not be null");
		requireNonNull(instance, "instance must not be null");
		requireNonNull(invocationCallback, "invocationCallback must not be null");

		final var classLoader = Thread.currentThread().getContextClassLoader();
		final var interfaces = new Class[]{iface};

		return (I) Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
			final var invocation = new Invocation(proxy, method, instance, args);

			return invocationCallback.onInvocation(invocation);
		});
	}

	private InvocationProxy() {
	}

}
