package com.tagadvance.cache;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

record CacheKey(Method method, Object[] args) {

	CacheKey(final Method method, final Object[] args) {
		this.method = requireNonNull(method, "method must not be null");
		this.args = requireNonNull(args, "args must not be null");
	}

	Method getMethod() {
		return method;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof final CacheKey key) {
			return CacheUtils.methodSignatureEquals(method, key.method) && Objects.deepEquals(args,
				key.args);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(CacheUtils.methodHashCode(method), Arrays.hashCode(args));
	}

}
