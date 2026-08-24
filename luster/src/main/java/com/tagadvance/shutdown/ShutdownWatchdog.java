package com.tagadvance.shutdown;

import static java.util.Objects.requireNonNull;

import com.tagadvance.stack.StackTraces;
import com.tagadvance.utilities.Sleep;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reports the threads still running some time after a shutdown was requested, with their stack
 * traces pruned, so the frame that needs to become interruptible can be named.
 *
 * <pre>{@code
 * try (final var ignored = ShutdownWatchdog.armed(Duration.ofSeconds(10), System.err::println)) {
 * 	executor.shutdown();
 * 	executor.awaitTermination(30, TimeUnit.SECONDS);
 * }   // reports anything still running, then disarms
 * }</pre>
 *
 * <p><strong>Do not drive this from a {@link Runtime#addShutdownHook(Thread) shutdown hook}
 * alone.</strong> The JVM begins shutdown when {@code System.exit} is called or a signal arrives,
 * or when the last non-daemon thread finishes. A non-daemon thread that hangs while nothing calls
 * exit means shutdown never begins, so the hook never runs — the hang that keeps the JVM alive
 * forever is precisely the hang a hook cannot see. Arm this when shutdown is <em>requested</em>,
 * from wherever that request originates.
 *
 * <p>The watchdog's own thread is a daemon, so it never holds the JVM open itself.
 *
 * <p>The report goes to a {@link Consumer} rather than a logger on purpose: a hang during shutdown
 * may well be <em>in</em> the logging subsystem, in which case a logged report is the one thing
 * guaranteed not to come out. {@code System.err::println} is a reasonable default.
 *
 * <p>It reports and does nothing else. Interrupting a straggler is a policy decision about which
 * victim to sacrifice, and abandons state that thread's code never expected to abandon.
 */
public final class ShutdownWatchdog implements AutoCloseable {

	/**
	 * How long {@link #close()} waits for an in-flight report before giving up, so a blocked
	 * consumer cannot delay shutdown indefinitely.
	 */
	private static final Duration REPORT_GRACE = Duration.ofSeconds(5);

	/**
	 * Frames matching this are dropped from every reported stack trace, so what is left is the
	 * application code that needs to become interruptible.
	 */
	private static final Pattern NOISE = StackTraces.JDK_PACKAGES;

	private final AtomicBoolean settled = new AtomicBoolean();

	private final CountDownLatch reported = new CountDownLatch(1);

	private final Thread watcher;

	/**
	 * Arms a watchdog that reports every live non-daemon thread other than itself.
	 *
	 * @param timeout how long to wait before reporting
	 * @param report  where the report goes
	 * @return the armed watchdog, disarmed by {@link #close()}
	 */
	public static ShutdownWatchdog armed(final Duration timeout, final Consumer<String> report) {
		return armed(timeout, report, thread -> !thread.isDaemon());
	}

	/**
	 * @param timeout how long to wait before reporting
	 * @param report  where the report goes
	 * @param filter  which threads are worth reporting; the watchdog's own thread is always
	 *                excluded
	 * @return the armed watchdog, disarmed by {@link #close()}
	 */
	public static ShutdownWatchdog armed(final Duration timeout, final Consumer<String> report,
		final Predicate<Thread> filter) {
		return new ShutdownWatchdog(timeout, report, filter);
	}

	private ShutdownWatchdog(final Duration timeout, final Consumer<String> report,
		final Predicate<Thread> filter) {
		requireNonNull(timeout, "timeout must not be null");
		requireNonNull(report, "report must not be null");
		requireNonNull(filter, "filter must not be null");

		this.watcher = new Thread(() -> {
			try {
				Sleep.ofThread().sleep(timeout);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();

				return;
			}

			if (settled.compareAndSet(false, true)) {
				try {
					report.accept(describe(timeout, filter));
				} finally {
					reported.countDown();
				}
			}
		}, "ShutdownWatchdog");
		watcher.setDaemon(true);
		watcher.start();
	}

	/**
	 * Disarms the watchdog. If it has already begun reporting, waits a bounded time for the report
	 * to be delivered — otherwise a report started just before shutdown completes would race the
	 * JVM's exit and be lost.
	 */
	@Override
	public void close() {
		if (settled.compareAndSet(false, true)) {
			watcher.interrupt();

			return;
		}

		try {
			reported.await(REPORT_GRACE.toMillis(), TimeUnit.MILLISECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	static String describe(final Duration timeout, final Predicate<Thread> filter) {
		final var self = Thread.currentThread();
		final var threads = Thread.getAllStackTraces()
			.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != self)
			.filter(entry -> entry.getKey().isAlive())
			.filter(entry -> filter.test(entry.getKey()))
			.sorted(Comparator.comparing(entry -> entry.getKey().getName()))
			.map(entry -> render(entry.getKey(), entry.getValue()))
			.collect(Collectors.joining(System.lineSeparator()));

		if (threads.isEmpty()) {
			return "No threads were still running %s after shutdown was requested.".formatted(
				timeout);
		}

		return "Threads still running %s after shutdown was requested:%s%s".formatted(timeout,
			System.lineSeparator(), threads);
	}

	private static String render(final Thread thread, final StackTraceElement[] stackTrace) {
		final var interesting = StackTraces.retainElement(NOISE).negate();
		final var frames = Stream.of(stackTrace)
			.filter(interesting)
			.map(element -> "\t\tat " + element)
			.collect(Collectors.joining(System.lineSeparator()));

		return "\t\"%s\" %s%s%s".formatted(thread.getName(), thread.getState(),
			System.lineSeparator(), frames.isEmpty() ? "\t\t<no application frames>" : frames);
	}


}
