package com.tagadvance.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Represents a {@link Method#invoke(Object, Object...) method invocation}.
 * <p>
 * {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()} are overridden because a
 * record's generated versions compare an array component by identity, which would make two
 * invocations with equal arguments unequal and print {@code [Ljava.lang.Object;@1b6d}.
 * <p>
 * {@literal args} is stored without copying, so an interceptor can rewrite the caller's arguments
 * before delegating. That is deliberate.
 *
 * @param proxy    the proxy instance, or {@literal null} when constructed outside a proxy
 * @param method   the {@link Method method} to {@link Method#invoke(Object, Object...) invoke}
 * @param instance the object the method is invoked on, or {@literal null} for a pure fake
 * @param args     the arguments supplied to the method
 */
public record Invocation(@Nullable Object proxy, Method method, @Nullable Object instance,
						 @Nullable Object... args) {

	/**
	 * @param proxy    the proxy instance, or {@literal null} when constructed outside a proxy
	 * @param method   the {@link Method method} to invoke
	 * @param instance the object the method is invoked on, or {@literal null} for a pure fake
	 * @param args     the arguments supplied to the method; {@literal null} becomes empty
	 */
	public Invocation(final @Nullable Object proxy, final Method method,
		final @Nullable Object instance, final @Nullable Object... args) {
		this.proxy = proxy;
		this.method = Objects.requireNonNull(method, "method must not be null");
		this.instance = instance;
		this.args = Optional.ofNullable(args).orElse(new Object[]{});
	}

	/**
	 * This is essentially the same as calling <code>instance.method(args);</code>
	 * <p>
	 * {@link IllegalAccessException} is unlikely to be thrown as, if necessary, the
	 * {@link Method method} will automatically set to {@literal accessible}.
	 * <p>
	 * {@link InvocationTargetException}, if caught, is automatically unwrapped and the
	 * {@link Throwable#getCause() cause} will be thrown instead.
	 *
	 * @return the result of the {@link Method#invoke(Object, Object...) method invocation}
	 * @throws Throwable                 It could be anything. Who knows? The Lord knows.
	 * @throws ProxyInvocationException  if there is no instance to delegate to
	 * @see Method#invoke(Object, Object...)
	 */
	public Object invoke() throws Throwable {
		// a static method is legitimately invoked against a null instance
		if (instance == null && !Modifier.isStatic(method.getModifiers())) {
			throw new ProxyInvocationException(
				"%s has no instance to delegate to; this proxy is a pure fake, so its interceptor must handle every invocation".formatted(
					method));
		}

		if (!method.canAccess(instance)) {
			method.trySetAccessible();
		}

		try {
			return method.invoke(instance, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

	@Override
	public boolean equals(final Object o) {
		return this == o || o instanceof final Invocation other && Objects.equals(proxy, other.proxy)
			&& Objects.equals(method, other.method) && Objects.equals(instance, other.instance)
			&& Arrays.deepEquals(args, other.args);
	}

	@Override
	public int hashCode() {
		return Objects.hash(proxy, method, instance, Arrays.deepHashCode(args));
	}

	@Override
	public String toString() {
		return "%s#%s(%s)".formatted(method.getDeclaringClass().getSimpleName(), method.getName(),
			Arrays.deepToString(args));
	}

}
