package com.tagadvance.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
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
		final var target = resolve(method, instance);

		// a static method is legitimately invoked against a null instance
		if (instance == null && !Modifier.isStatic(target.getModifiers())) {
			throw new ProxyInvocationException(
				"%s has no instance to delegate to; this proxy is a pure fake, so its interceptor must handle every invocation".formatted(
					target));
		}

		if (!target.canAccess(instance)) {
			target.trySetAccessible();
		}

		try {
			return target.invoke(instance, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

	/**
	 * Resolves {@literal method} against {@literal instance}.
	 * <p>
	 * A proxy may be created for a sub-interface of anything the instance actually implements —
	 * {@link InvocationProxy#createProxy} only requires {@code I extends T} — which is what lets a
	 * <em>mask</em> add annotations to an interface owned by a dependency. In that case the
	 * interface {@link Method} cannot be invoked against the instance directly, so it is resolved
	 * against the instance's own class instead.
	 *
	 * @param method   the method to resolve
	 * @param instance the object it will be invoked on, or {@literal null} for a static method
	 * @return {@literal method}, or the instance's own implementation of it
	 * @throws ProxyInvocationException if the instance has no such method
	 */
	public static Method resolve(final Method method, final @Nullable Object instance) {
		Objects.requireNonNull(method, "method must not be null");
		if (instance == null || method.getDeclaringClass().isInstance(instance)) {
			return method;
		}

		final var type = instance.getClass();
		try {
			return type.getMethod(method.getName(), method.getParameterTypes());
		} catch (final NoSuchMethodException ignored) {
			// the instance may carry only the erased override, e.g. Function.apply(Object)
			// implementing a sub-interface's apply(Integer)
		}

		final var candidates = Stream.of(type.getMethods())
			.filter(candidate -> candidate.getName().equals(method.getName()))
			.filter(candidate -> candidate.getParameterCount() == method.getParameterCount())
			.filter(candidate -> accepts(candidate, method))
			.toList();
		final var preferred = candidates.stream()
			.filter(candidate -> !candidate.isBridge())
			.toList();

		return (preferred.isEmpty() ? candidates : preferred).stream()
			.findFirst()
			.orElseThrow(() -> new ProxyInvocationException(
				"%s does not implement %s".formatted(type.getName(), method)));
	}

	/**
	 * @return {@literal true} if {@literal candidate} can accept everything {@literal method}
	 * declares
	 */
	private static boolean accepts(final Method candidate, final Method method) {
		final var candidateTypes = candidate.getParameterTypes();
		final var declaredTypes = method.getParameterTypes();
		for (int i = 0; i < candidateTypes.length; i++) {
			if (!candidateTypes[i].isAssignableFrom(declaredTypes[i])) {
				return false;
			}
		}

		return true;
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
