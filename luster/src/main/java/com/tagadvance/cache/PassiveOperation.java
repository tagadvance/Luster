package com.tagadvance.cache;

import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationInterceptor;

/**
 * Passes an un-annotated method straight through to the instance.
 */
final class PassiveOperation implements InvocationInterceptor {

	private static final PassiveOperation INSTANCE = new PassiveOperation();

	static InvocationInterceptor getInstance() {
		return INSTANCE;
	}

	private PassiveOperation() {
	}

	@Override
	public Object onInvocation(final Invocation invocation) throws Throwable {
		final var instance = invocation.instance();
		final var method = CacheUtils.resolve(invocation.method(), instance);

		return CacheUtils.invoke(method, instance, invocation.args());
	}

}
