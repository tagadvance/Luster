package com.tagadvance.cache;

import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationInterceptor;
import com.tagadvance.reflection.M;
import com.tagadvance.reflection.ReflectionException;
import java.util.concurrent.atomic.AtomicReference;

final class PassiveOperation implements InvocationInterceptor {

	private static final AtomicReference<PassiveOperation> instance = new AtomicReference<>();

	public static InvocationInterceptor getInstance() {
		return instance.updateAndGet(i -> i == null ? new PassiveOperation() : i);
	}

	private PassiveOperation() {

	}

	@Override
	public Object onInvocation(final Invocation invocation) throws Throwable {
		final var i = invocation.instance();
		final var method = invocation.method();
		final var args = invocation.args();

		try {
			return M.invoke(i, args).apply(method);
		} catch (final ReflectionException e) {
			throw CacheUtils.toValidException(e, method);
		}
	}

}
