package com.tagadvance.proxy;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;

public class InterceptorProxy<I> implements InvocationHandler {

	private final I instance;
	private final I proxy;
	private final LinkedList<Interceptor<I>> interceptors = new LinkedList<>();

	public static <I> I of(final Class<I> iface, final I instance,
		final Interceptor<I>... interceptors) {
		return new InterceptorProxy<>(iface, instance).withInterceptors(interceptors).getProxy();
	}

	public InterceptorProxy(final Class<I> iface, final I instance) {
		this.instance = requireNonNull(instance, "instance must not be null");
		this.proxy = createProxy(iface);
	}

	private I createProxy(final Class<I> iface) {
		requireNonNull(iface, "iface must not be null");

		final var classLoader = getClass().getClassLoader();
		final var interfaces = new Class[]{iface};

		return (I) Proxy.newProxyInstance(classLoader, interfaces, this);
	}

	// document fifo order, i.e. newest interceptor called first in the chain
	public InterceptorProxy<I> withInterceptors(final Interceptor<I>... interceptors) {
		final var list = Arrays.asList(interceptors);
		Collections.reverse(list);
		list.forEach(this::withInterceptor);

		return this;
	}

	// document lifo order, i.e. newest interceptor called first in the chain
	public InterceptorProxy<I> withInterceptor(final Interceptor<I> interceptor) {
		interceptors.push(interceptor);

		return this;
	}

	public I getProxy() {
		return proxy;
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
		throws Throwable {
		final var copyOfInterceptors = new LinkedList<>(interceptors);
		final var chain = new InterceptorChain<>(copyOfInterceptors);

		final var invocation = new Invocation<>(proxy, method, instance, args);

		return chain.proceed(invocation);
	}

	private static class InterceptorChain<I> implements Chain<I> {

		private final Deque<Invocation<I>> invocations = new LinkedList<>();
		private final Deque<Interceptor<I>> interceptors;

		private InterceptorChain(final Deque<Interceptor<I>> interceptors) {
			this.interceptors = interceptors;
		}

		@Override
		public Invocation<I> invocation() {
			return invocations.peek();
		}

		@Override
		public Object proceed(final Invocation<I> invocation) throws Throwable {
			if (interceptors.isEmpty()) {
				return invocation.invoke();
			}

			invocations.push(invocation);
			try {
				return interceptors.pop().intercept(this);
			} finally {
				invocations.pop();
			}
		}

	}

}
