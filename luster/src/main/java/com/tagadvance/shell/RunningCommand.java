package com.tagadvance.shell;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handle on a process started by {@link Command#start()}.
 *
 * <p>Each of the process' output streams is drained by a dedicated thread performing blocking
 * reads. A blocking read is the only way to observe end of stream: {@link java.io.Reader#ready()}
 * reports whether data is available right now, and at end of file that is indistinguishable from a
 * process that has simply gone quiet. Because the reads block, an idle process costs nothing.</p>
 *
 * <p>{@link #close()} terminates the process and waits, briefly, for the reader threads to finish,
 * so that a callback is not invoked after {@code close()} returns.</p>
 */
public final class RunningCommand implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(RunningCommand.class);

	/**
	 * Name prefix of the reader threads, which makes them identifiable in a thread dump.
	 */
	static final String THREAD_NAME_PREFIX = "Command";

	/**
	 * How long a destroyed process is given to exit on its own before it is killed.
	 */
	private static final Duration DESTROY_GRACE = Duration.ofSeconds(5);

	/**
	 * How long {@link #close()} waits for the reader threads after the process is gone.
	 */
	private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(5);

	private final Process process;

	private final ExecutorService readers;

	RunningCommand(final Process process, final Charset charset,
		final Consumer<String> outputConsumer, final Consumer<String> errorConsumer,
		final boolean redirectErrorStream) {
		this.process = process;

		final var pid = process.pid();
		final var counter = new AtomicInteger();
		this.readers = Executors.newCachedThreadPool(runnable -> {
			final var thread = new Thread(runnable,
				"%s-%d-%d".formatted(THREAD_NAME_PREFIX, pid, counter.incrementAndGet()));
			thread.setDaemon(true);

			return thread;
		});

		// standard input is not supported; closing it gives the child end of file instead of a
		// pipe that never delivers anything
		closeQuietly(process.getOutputStream());

		readers.execute(() -> pump(process.getInputStream(), charset, outputConsumer));
		if (!redirectErrorStream) {
			readers.execute(() -> pump(process.getErrorStream(), charset, errorConsumer));
		}

		// no further tasks are submitted, so the pool may retire itself; this also makes
		// awaitTermination meaningful without an explicit shutdown
		readers.shutdown();
	}

	/**
	 * @return {@code true} while the process has not yet exited
	 */
	public boolean isAlive() {
		return process.isAlive();
	}

	/**
	 * @return the native process id
	 */
	public long pid() {
		return process.pid();
	}

	/**
	 * Wait for the process to exit and for its output to be fully delivered.
	 *
	 * @param timeout the maximum time to wait
	 * @return the exit code, where {@code 0} conventionally means success
	 * @throws InterruptedException if the calling thread is interrupted
	 * @throws TimeoutException     if the process is still running when the timeout elapses
	 */
	public int awaitExit(final Duration timeout) throws InterruptedException, TimeoutException {
		final var timeoutMillis = timeout.toMillis();
		final var deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

		if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
			throw new TimeoutException(
				"process %d did not exit within %s".formatted(process.pid(), timeout));
		}

		// the process is gone but its last lines may still be in the pipe buffer
		final var remaining = deadline - System.nanoTime();
		if (remaining > 0 && !readers.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
			logger.warn("output of process {} was not fully consumed within {}", process.pid(),
				timeout);
		}

		return process.exitValue();
	}

	/**
	 * Terminate the process and release its reader threads. The process is asked to exit, then
	 * killed if it has not done so within a grace period. Closing an already exited process only
	 * releases the readers.
	 */
	@Override
	public void close() {
		process.destroy();

		try {
			if (!process.waitFor(DESTROY_GRACE.toMillis(), TimeUnit.MILLISECONDS)) {
				logger.warn("process {} ignored destroy; killing it", process.pid());
				process.destroyForcibly();
				process.waitFor(DESTROY_GRACE.toMillis(), TimeUnit.MILLISECONDS);
			}

			// the pipes are closed now, so the readers see end of file; the interrupt is only a
			// backstop for a callback that is misbehaving
			readers.shutdownNow();
			if (!readers.awaitTermination(DRAIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
				logger.warn("reader threads of process {} did not stop within {}", process.pid(),
					DRAIN_TIMEOUT);
			}
		} catch (final InterruptedException e) {
			readers.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private static void pump(final InputStream in, final Charset charset,
		final Consumer<String> lineConsumer) {
		try (final var reader = new BufferedReader(new InputStreamReader(in, charset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				accept(lineConsumer, line);
			}
		} catch (final IOException e) {
			// expected when close() destroys the process mid-read
			logger.debug("stopped reading process output: {}", e.getMessage());
		}
	}

	private static void accept(final Consumer<String> lineConsumer, final String line) {
		try {
			lineConsumer.accept(line);
		} catch (final RuntimeException e) {
			// keep draining: an unread pipe eventually blocks the child
			logger.error("line consumer threw while handling \"{}\"", line, e);
		}
	}

	private static void closeQuietly(final AutoCloseable closeable) {
		try {
			closeable.close();
		} catch (final Exception e) {
			logger.debug("failed to close {}: {}", closeable, e.getMessage());
		}
	}

}
