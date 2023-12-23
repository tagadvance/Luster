package com.tagadvance.mockingbird;

import com.tagadvance.proxy.Invocation;
import com.tagadvance.reflection.M;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Generates a {@link String name} from an {@link Invocation invocation}.
 */
public class CountingInvocationNameGenerator implements InvocationNameGenerator {

	private final AtomicInteger count = new AtomicInteger();

	@Override
	public String toName(final Invocation invocation) {
		final var i = count.getAndIncrement();
		final var simpleName = invocation.instance().getClass().getSimpleName();
		final var method = invocation.method();
		final var methodName = method.getName();
		final var parameters = M.hasParameterCount(0).test(method) ? ""
			: "." + M.getParameterTypes(method)
				.map(Class::getSimpleName)
				.collect(Collectors.joining("_"));
		return "%04d.%s.%s%s.mimic".formatted(i, simpleName, methodName, parameters);
	}

}
