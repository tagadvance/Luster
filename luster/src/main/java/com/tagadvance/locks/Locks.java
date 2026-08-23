package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Creates {@link RWLock locks}.
 */
public final class Locks {

	/**
	 * @return a new {@link RWLock} backed by a {@link ReentrantReadWriteLock}
	 */
	public static RWLock newLock() {
		return wrap(new ReentrantReadWriteLock());
	}

	/**
	 * @param lock the {@link ReadWriteLock} to adapt
	 * @return an {@link RWLock} delegating to {@literal lock}
	 */
	public static RWLock wrap(final ReadWriteLock lock) {
		requireNonNull(lock, "lock must not be null");

		return () -> lock;
	}

	private Locks() {
	}

}
