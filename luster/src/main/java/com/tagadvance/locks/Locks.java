package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Creates {@link ScopedLock locks}.
 */
public final class Locks {

	/**
	 * @return a new {@link ScopedLock} backed by a {@link ReentrantReadWriteLock}
	 */
	public static ScopedLock newLock() {
		return wrap(new ReentrantReadWriteLock());
	}

	/**
	 * @param lock the {@link ReadWriteLock} to adapt
	 * @return an {@link ScopedLock} delegating to {@literal lock}
	 */
	public static ScopedLock wrap(final ReadWriteLock lock) {
		requireNonNull(lock, "lock must not be null");

		return () -> lock;
	}

	private Locks() {
	}

}
