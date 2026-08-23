package com.tagadvance.proxy;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Create {@link Proxy proxies} with an {@link InvocationInterceptor invocation callback}.
 */
public final class InvocationProxy {

	/**
	 * @param iface                an interface
	 * @param instance             an instance of {@literal iface}, or {@literal null} for a pure
	 *                             fake whose interceptor handles every call
	 * @param interceptor          an {@link InvocationInterceptor}
	 * @param additionalInterfaces further interfaces the proxy should implement
	 * @param <T>                  the instance type
	 * @param <I>                  the interface type
	 * @return a proxy
	 */
	@SuppressWarnings("unchecked")
	public static <T, I extends T> I createProxy(final Class<I> iface, final @Nullable T instance,
		final InvocationInterceptor interceptor, final Class<?>... additionalInterfaces) {
		requireNonNull(iface, "iface must not be null");
		requireNonNull(interceptor, "interceptor must not be null");
		requireNonNull(additionalInterfaces, "additionalInterfaces must not be null");

		// the interface's own loader, not the thread's: the context classloader is whatever the
		// calling thread happened to be configured with, and in a container it may not see iface
		// at all, or may resolve a different class of the same name
		final var classLoader = Optional.ofNullable(iface.getClassLoader())
			.orElseGet(InvocationProxy.class::getClassLoader);
		final var interfaces = Stream.concat(Stream.of(iface), Stream.of(additionalInterfaces))
			.distinct()
			.toArray(Class<?>[]::new);

		return (I) Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
			if (method.getDeclaringClass() == Object.class) {
				return onObjectMethod(proxy, instance, iface, method, args);
			}

			final var invocation = new Invocation(proxy, method, instance, args);
			try {
				return interceptor.onInvocation(invocation);
			} catch (final Throwable throwable) {
				throw conforming(method, throwable);
			}
		});
	}

	/**
	 * {@link Proxy} routes only {@code equals}, {@code hashCode} and {@code toString} here.
	 * <p>
	 * Equality is the proxy's own identity rather than the instance's: delegating it would make
	 * {@code proxy.equals(instance)} true while {@code instance.equals(proxy)} stayed false, which
	 * breaks the symmetry {@link Object#equals(Object)} requires. {@code toString} is delegated,
	 * because that is what you want in a debugger.
	 */
	private static Object onObjectMethod(final Object proxy, final @Nullable Object instance,
		final Class<?> iface, final Method method, final Object @Nullable [] args) {
		return switch (method.getName()) {
			case "equals" -> proxy == (args == null ? null : args[0]);
			case "hashCode" -> System.identityHashCode(proxy);
			case "toString" -> instance == null ? "%s@%s".formatted(iface.getSimpleName(),
				Integer.toHexString(System.identityHashCode(proxy))) : instance.toString();
			default -> throw new ProxyInvocationException(
				"unexpected Object method %s".formatted(method));
		};
	}

	/**
	 * Reshapes a throwable so the caller's {@code catch} blocks can see it.
	 * <p>
	 * An interceptor that throws a checked exception the method does not declare would otherwise
	 * reach the caller as a {@link java.lang.reflect.UndeclaredThrowableException} with a null
	 * message. If the causal chain holds something the method does declare, that is thrown
	 * instead; otherwise the wrapper at least names what happened.
	 */
	private static Throwable conforming(final Method method, final Throwable throwable) {
		if (throwable instanceof RuntimeException || throwable instanceof Error) {
			return throwable;
		}

		final var declared = method.getExceptionTypes();
		var current = throwable;
		while (current != null) {
			if (isDeclared(declared, current)) {
				return current;
			}

			final var cause = current.getCause();
			current = cause == current ? null : cause;
		}

		return new ProxyInvocationException(
			"%s does not declare %s, thrown by the interceptor".formatted(method,
				throwable.getClass().getName()), throwable);
	}

	private static boolean isDeclared(final Class<?>[] declared, final Throwable throwable) {
		return Arrays.stream(declared).anyMatch(type -> type.isInstance(throwable));
	}

	private InvocationProxy() {
	}

}
