package com.tagadvance.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Thrown when an interceptor throws a checked exception that the proxied {@link Method} does not
 * declare, and nothing in its causal chain is declared either.
 * <p>
 * This replaces {@link UndeclaredThrowableException}, whose message is {@literal null} and whose
 * cause is only reachable through {@code getUndeclaredThrowable()}.
 */
public class ProxyInvocationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	ProxyInvocationException(final String message) {
		super(message);
	}

	ProxyInvocationException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
