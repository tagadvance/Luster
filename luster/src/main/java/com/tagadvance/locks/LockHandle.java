package com.tagadvance.locks;

import java.util.concurrent.locks.Lock;

/**
 * A held {@link Lock}, released by {@link #close()}. Intended for try-with-resources:
 *
 * <pre>{@code
 * try (final var ignored = lock.write()) {
 * 	map.put(key, value);
 * }
 * }</pre>
 * <p>
 * {@link #close()} deliberately narrows {@link AutoCloseable#close()} to throw nothing, so a
 * try-with-resources block over one of these does not have to catch anything.
 */
@FunctionalInterface
public interface LockHandle extends AutoCloseable {

	/**
	 * Releases the lock.
	 */
	@Override
	void close();

}
