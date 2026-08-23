package com.tagadvance.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

final class CacheUtils {

	/**
	 * The proxied interface may be a sub-interface of anything the instance actually implements —
	 * {@code newCache(Class<I>, T)} only requires {@code I extends T} — so an interface
	 * {@link Method} cannot always be invoked against the instance directly. Resolve it against
	 * the instance's own class when that is the case.
	 */
	static Method resolve(final Method method, final Object instance) {
		if (method.getDeclaringClass().isInstance(instance)) {
			return method;
		}

		final var type = instance.getClass();
		try {
			return type.getMethod(method.getName(), method.getParameterTypes());
		} catch (final NoSuchMethodException ignored) {
			// the instance may only carry the erased override, e.g. Function.apply(Object)
			// implementing a sub-interface's apply(Integer)
		}

		final var candidates = Stream.of(type.getMethods())
			.filter(m -> m.getName().equals(method.getName()))
			.filter(m -> m.getParameterCount() == method.getParameterCount())
			.filter(m -> accepts(m, method))
			.toList();
		final var preferred = candidates.stream().filter(m -> !m.isBridge()).toList();

		return (preferred.isEmpty() ? candidates : preferred).stream()
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
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

	/**
	 * Invokes {@literal method}, unwrapping the {@link InvocationTargetException} so that what the
	 * real method threw is what propagates.
	 */
	static Object invoke(final Method method, final Object instance, final Object[] args)
		throws Throwable {
		if (!method.canAccess(instance)) {
			method.trySetAccessible();
		}

		try {
			return method.invoke(instance, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private CacheUtils() {
	}

}
