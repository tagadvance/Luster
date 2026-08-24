package com.tagadvance.locks;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks who holds and who waits for the {@link ScopedLock locks} created by
 * {@link Locks#instrumented(LockRegistry)}, so that {@link DeadlockDetector} can find cycles the
 * JVM cannot.
 * <p>
 * {@link java.lang.management.ThreadMXBean#findDeadlockedThreads()} already finds cycles through
 * both {@code synchronized} monitors and {@code java.util.concurrent} locks, and should be
 * preferred for those. It is blind to one case: AQS records only an <em>exclusive</em> owner, so a
 * lock held in shared mode by any number of readers records no owner at all and the JVM has no
 * edge to follow. Two threads each holding a read lock and each waiting for the other's write lock
 * deadlock permanently and are reported as no deadlock at all.
 * <p>
 * Recording shared-mode holders is exactly the bookkeeping AQS omits for performance, which is why
 * instrumentation is opt-in and why {@link Locks#newLock()} is left completely untouched.
 */
public final class LockRegistry {

	private final ConcurrentMap<Thread, Wait> waits = new ConcurrentHashMap<>();

	/**
	 * @return a new, empty registry
	 */
	public static LockRegistry create() {
		return new LockRegistry();
	}

	private LockRegistry() {
	}

	void waiting(final Thread thread, final InstrumentedScopedLock lock, final boolean exclusive) {
		waits.put(thread, new Wait(lock, exclusive));
	}

	void doneWaiting(final Thread thread) {
		waits.remove(thread);
	}

	/**
	 * @return every thread that is currently blocked on an instrumented lock, with what it wants
	 */
	Map<Thread, Wait> waits() {
		return Map.copyOf(waits);
	}

	/**
	 * @return the cycles currently present, empty if there are none
	 */
	List<List<Thread>> cycles() {
		final var snapshot = waits();
		final var cycles = new ArrayList<List<Thread>>();
		final var settled = new LinkedHashSet<Thread>();

		for (final var thread : snapshot.keySet()) {
			if (settled.contains(thread)) {
				continue;
			}

			// path doubles as the "currently being explored" set; a hit inside it is a cycle
			final var path = new ArrayList<Thread>();
			walk(thread, snapshot, path, settled, cycles);
		}

		return List.copyOf(cycles);
	}

	private static void walk(final Thread thread, final Map<Thread, Wait> snapshot,
		final List<Thread> path, final Set<Thread> settled, final List<List<Thread>> cycles) {
		final var index = path.indexOf(thread);
		if (index >= 0) {
			cycles.add(List.copyOf(path.subList(index, path.size())));

			return;
		}

		if (settled.contains(thread)) {
			return;
		}

		final var wait = snapshot.get(thread);
		if (wait == null) {
			// not blocked, so it is not part of a cycle and every path through it ends here
			settled.add(thread);

			return;
		}

		path.add(thread);
		for (final var blocker : wait.blockers(thread)) {
			walk(blocker, snapshot, path, settled, cycles);
		}

		path.remove(path.size() - 1);
		settled.add(thread);
	}

	/**
	 * What a blocked thread is waiting for.
	 *
	 * @param lock      the lock it wants
	 * @param exclusive whether it wants the write lock
	 */
	record Wait(InstrumentedScopedLock lock, boolean exclusive) {

		Wait {
			requireNonNull(lock, "lock must not be null");
		}

		/**
		 * A writer is blocked by every holder; a reader is blocked only by the writer, because
		 * readers do not exclude each other. Treating both alike would report cycles that are not
		 * cycles.
		 *
		 * @param waiter the blocked thread, excluded from its own blockers unless it is
		 *               deadlocking against itself by attempting an upgrade
		 * @return the threads standing in {@literal waiter}'s way
		 */
		Set<Thread> blockers(final Thread waiter) {
			final var blockers = new LinkedHashSet<>(lock.writers());
			if (exclusive) {
				blockers.addAll(lock.readers());
			}

			// a read-to-write upgrade deadlocks a thread against itself, which is a real cycle
			if (!(exclusive && lock.readers().contains(waiter))) {
				blockers.remove(waiter);
			}

			return blockers;
		}

	}

}
