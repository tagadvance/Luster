package com.tagadvance.mockingbird;

import com.tagadvance.proxy.Invocation;

/**
 * Generates a file name for an {@link Invocation invocation}.
 * <p>
 * The name must be a valid file name, and it must be <strong>deterministic across JVM runs</strong>
 * — anything derived from an identity hash will produce a different name every time the program
 * starts, so recordings will never be found again. The name is resolved inside a directory named
 * for the interface, so it does not need to identify the interface itself.
 */
public interface InvocationNameGenerator {

	/**
	 * @param iface      the interface type
	 * @param invocation an {@link Invocation invocation}
	 * @return a file name for the supplied {@link Invocation invocation}
	 */
	String toName(Class<?> iface, Invocation invocation);

}
