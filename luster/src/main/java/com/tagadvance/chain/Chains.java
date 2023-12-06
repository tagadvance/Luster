package com.tagadvance.chain;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Chains {

	public static <R, E extends Throwable> Chain<R, E> synchronizedChain(final Chain<R, E> chain) {
		return new SynchronizedChain(chain);
	}

	private Chains() {
	}

	private static class SynchronizedChain<R, E extends Throwable> implements Chain<R, E> {

		private final ReadWriteLock lock = new ReentrantReadWriteLock();
		private final Chain<R, E> chain;

		private SynchronizedChain(final Chain<R, E> chain) {
			this.chain = requireNonNull(chain, "chain must not be null");
		}

		@Override
		public ChainCallable<R, E> input() {
			lock.readLock().lock();
			try {
				return chain.input();
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public R next(final ChainCallable<R, E> callable) throws E {
			lock.writeLock().lock();
			try {
				return chain.next(callable);
			} finally {
				lock.writeLock().unlock();
			}
		}

	}

}
