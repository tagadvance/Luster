package com.tagadvance.shell;

import com.tagadvance.exception.CheckedSupplier;
import com.tagadvance.utilities.Sleep;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Exec} efficiently reads the output from multiple processes using a single thread.
 */
public final class Exec implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(Exec.class);

	private static final Sleep sleep = Thread::sleep;

	private final ExecutorService service = Executors.newSingleThreadExecutor();

	private final AtomicReference<Future<?>> future = new AtomicReference<>();

	private final AtomicBoolean isAlive = new AtomicBoolean(true);

	// use a separate list to avoid ConcurrentModificationException
	// more efficient than CopyOnWriteArrayList due to removals
	private final List<Resource> resourceQueue = Collections.synchronizedList(new ArrayList<>());

	private final List<Resource> resources = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Default constructor.
	 */
	public Exec() {

	}

	/**
	 * Start and follow the output from a process.
	 *
	 * @param processSupplier the process supplier
	 * @param mixedConsumer   a callback that will receive the output from both STDOUT and STDERR.
	 * @throws IOException if an I/O error occurs
	 */
	public void start(final CheckedSupplier<Process, IOException> processSupplier,
		final Consumer<String> mixedConsumer) throws IOException {
		start(processSupplier, mixedConsumer, mixedConsumer);
	}

	/**
	 * Start and follow the output from a process.
	 *
	 * @param processSupplier the process supplier
	 * @param outConsumer     a callback that will receive the output from both STDOUT
	 * @param errConsumer     a callback that will receive the output from STDERR
	 * @throws IOException if an I/O error occurs
	 */
	public void start(final CheckedSupplier<Process, IOException> processSupplier,
		final Consumer<String> outConsumer, final Consumer<String> errConsumer) throws IOException {
		if (!isAlive.get()) {
			throw new IOException("%s is already closed".formatted(getClass().getSimpleName()));
		}

		final var process = processSupplier.get();

		final var in = process.getInputStream();
		enqueue(in, outConsumer);

		final var err = process.getErrorStream();
		enqueue(err, errConsumer);

		future.updateAndGet(f -> f == null ? service.submit(this::startFlush) : f);
	}

	private void enqueue(final InputStream in, final Consumer<String> lineConsumer) {
		final var reader = new InputStreamReader(in);
		final var buffer = new BufferedReader(reader);
		resourceQueue.add(new Resource(buffer, lineConsumer));
	}

	private void startFlush() {
		while (isAlive.get()) {
			if (flush() == 0 && isAlive.get()) {
				// busy-wait
				if (!sleep.slept(1)) {
					break;
				}
			}
		}
	}

	private int flush() {
		int lineCount = 0;

		resourceQueue.removeIf(resources::add);

		final var i = resources.listIterator();
		while (isAlive.get() && i.hasNext()) {
			lineCount += i.next().processReadyLines(i::remove);
		}

		return lineCount;
	}

	@Override
	public void close() {
		service.shutdown();
		isAlive.set(false);
		future.set(null);
		resourceQueue.removeIf(Resource::closeQuietly);
		resources.removeIf(Resource::closeQuietly);
	}

	private record Resource(BufferedReader reader, Consumer<String> lineConsumer) {

		private int processReadyLines(final Runnable remover) {
			int lineCount = 0;

			try {
				while (reader.ready()) {
					final var line = reader.readLine();
					if (line == null) {
						closeQuietly();
						remover.run();
					} else {
						lineConsumer.accept(line);
						lineCount++;
					}
				}
			} catch (final IOException e) {
				logger.error("Failed to read line \"{}\"", e.getMessage());

				closeQuietly();
				remover.run();
			}

			return lineCount;
		}

		private boolean closeQuietly() {
			try {
				reader.close();
			} catch (final IOException ee) {
				// do nothing
			}

			return true;
		}

	}

}
