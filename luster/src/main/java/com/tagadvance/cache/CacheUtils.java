package com.tagadvance.cache;

import com.google.common.base.Throwables;
import com.tagadvance.reflection.ReflectionException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class CacheUtils {

	private CacheUtils() {

	}

	static Throwable toValidException(final ReflectionException e, final Method method) {
		return Throwables.getCausalChain(e)
			.stream()
			.skip(1)
			.filter(exceptionMatchesMethodSignature(method))
			.findFirst()
			.orElse(e);
	}

	private static Predicate<Throwable> exceptionMatchesMethodSignature(final Method method) {
		final var exceptionTypes = method.getExceptionTypes();

		return t -> t instanceof RuntimeException || Stream.of(exceptionTypes)
			.anyMatch(et -> et.isInstance(t));
	}

	static int methodHashCode(final Method method) {
		return Objects.hash(method.getReturnType(), method.getName(),
			Arrays.hashCode(method.getParameterTypes()));
	}

	static boolean methodSignatureEquals(final Method method, final Method otherMethod) {
		// TODO: research method.getAnnotatedReturnType() and method.getGenericReturnType()
		return Objects.equals(method.getReturnType(), otherMethod.getReturnType())
			&& Objects.equals(method.getName(), otherMethod.getName()) && methodSignatureMatches(
			method, otherMethod);
	}

	private static boolean methodSignatureMatches(final Method method, final Method otherMethod) {
		if (method.getParameterCount() != otherMethod.getParameterCount()) {
			return false;
		}

		final var parameterTypes1 = method.getParameterTypes();
		final var parameterTypes2 = otherMethod.getParameterTypes();
		for (int i = 0; i < parameterTypes1.length; i++) {
			if (!parameterTypes1[i].isAssignableFrom(parameterTypes2[i])) {
				return false;
			}
		}

		return true;
	}

}
