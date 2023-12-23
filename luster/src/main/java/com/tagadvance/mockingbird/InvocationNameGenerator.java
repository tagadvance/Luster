package com.tagadvance.mockingbird;

import com.tagadvance.proxy.Invocation;

/**
 * Generates a {@link String name} from an {@link Invocation invocation}. The method used to
 * generate the name must be deterministic! The name must be a valid file name. It is left to the
 * implementor to ensure that the file name is compatible with the underlying filesystem.
 */
public interface InvocationNameGenerator {

	/**
	 * @param invocation an {@link Invocation invocation}
	 * @return a {@link String name} for the supplied {@link Invocation invocation}
	 */
	String toName(final Invocation invocation);

}
