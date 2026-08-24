package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A cycle of threads, each waiting for a lock held by the next.
 * <p>
 * A cycle of one is not a mistake: {@link java.util.concurrent.locks.ReentrantReadWriteLock} does
 * not permit upgrading a read lock to a write lock, so a thread that tries deadlocks against
 * itself.
 *
 * @param threads the cycle, in wait-for order
 */
public record Deadlock(List<Thread> threads) {

	/**
	 * @param threads the cycle, in wait-for order
	 * @throws IllegalArgumentException if {@literal threads} is empty
	 */
	public Deadlock {
		requireNonNull(threads, "threads must not be null");
		if (threads.isEmpty()) {
			throw new IllegalArgumentException("threads must not be empty");
		}

		threads = List.copyOf(threads);
	}

	/**
	 * @return {@literal true} if a single thread is deadlocked against itself, which means it
	 * attempted to upgrade a read lock it already holds
	 */
	public boolean isUpgrade() {
		return threads.size() == 1;
	}

	@Override
	public String toString() {
		return threads.stream()
			.map(Thread::getName)
			.collect(Collectors.joining(" -> ", "[", isUpgrade() ? " (read-to-write upgrade)]"
				: " -> " + threads.get(0).getName() + "]"));
	}

}
