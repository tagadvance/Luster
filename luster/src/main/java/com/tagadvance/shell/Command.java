package com.tagadvance.shell;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A specification for an external process whose output is delivered to {@link Consumer line
 * consumers}. {@link #start()} runs it and returns a {@link RunningCommand handle} for awaiting the
 * exit code and terminating the process.
 *
 * <pre>{@code
 * try (final var command = Command.of("tail", "-n0", "-F", path.toString())
 * 		.charset(StandardCharsets.UTF_8)
 * 		.onOutput(logger::info)
 * 		.onError(logger::warn)
 * 		.start()) {
 * 	final int exit = command.awaitExit(Duration.ofMinutes(5));
 * }
 * }</pre>
 *
 * <p>A started command reads each stream on its own blocking thread, so end of stream is detected
 * as soon as the process closes it. Instances of this class are not thread safe; configure a
 * command on one thread and then {@link #start() start} it. A single specification may be started
 * more than once, each start producing an independent process.</p>
 *
 * <p><strong>Standard input is not supported.</strong> The child's standard input is closed
 * immediately after the process starts, so a child that reads from it sees end of file rather than
 * blocking forever on a pipe nobody writes to. Use a shell redirect or a temporary file if a child
 * needs input.</p>
 */
public final class Command {

	private static final Consumer<String> DISCARD = line -> {
		// discard
	};

	private final List<String> command;

	private Charset charset = nativeCharset();

	private Consumer<String> outputConsumer = DISCARD;

	private Consumer<String> errorConsumer = DISCARD;

	private boolean redirectErrorStream;

	/**
	 * Create a command from an executable and its arguments. The executable is resolved against
	 * {@code PATH} unless it is an absolute path.
	 *
	 * @param command the executable followed by its arguments
	 * @return a new command
	 * @throws IllegalArgumentException if {@code command} is empty
	 */
	public static Command of(final String... command) {
		return of(List.of(command));
	}

	/**
	 * Create a command from an executable and its arguments. The executable is resolved against
	 * {@code PATH} unless it is an absolute path.
	 *
	 * @param command the executable followed by its arguments
	 * @return a new command
	 * @throws IllegalArgumentException if {@code command} is empty
	 */
	public static Command of(final List<String> command) {
		final var copy = List.copyOf(command);
		if (copy.isEmpty()) {
			throw new IllegalArgumentException("command must contain at least an executable");
		}

		return new Command(copy);
	}

	private Command(final List<String> command) {
		this.command = command;
	}

	/**
	 * Set the {@link Charset charset} used to decode the process output.
	 *
	 * <p>The default is the JVM's native encoding, i.e. the {@code native.encoding} system
	 * property, which is what the operating system hands to a child process. Since JDK 18 that is
	 * no longer the same as {@code file.encoding}, which now defaults to UTF-8 regardless of
	 * platform, so neither {@link Charset#defaultCharset()} nor the no-arg
	 * {@link java.io.InputStreamReader} constructor is a safe default for subprocess output.</p>
	 *
	 * @param charset the charset
	 * @return this command
	 */
	public Command charset(final Charset charset) {
		this.charset = Objects.requireNonNull(charset, "charset must not be null");

		return this;
	}

	/**
	 * Set the callback that receives each line written to standard output.
	 *
	 * <p>The callback is invoked on the reader thread, so it must not block for long. An exception
	 * thrown by the callback is logged and the line is dropped; reading continues, because
	 * abandoning the stream would eventually block the child on a full pipe.</p>
	 *
	 * @param outputConsumer the callback
	 * @return this command
	 */
	public Command onOutput(final Consumer<String> outputConsumer) {
		this.outputConsumer = Objects.requireNonNull(outputConsumer,
			"outputConsumer must not be null");

		return this;
	}

	/**
	 * Set the callback that receives each line written to standard error. It never fires when
	 * {@link #redirectErrorStream(boolean)} is enabled.
	 *
	 * <p>The callback is invoked on the reader thread, subject to the same constraints as
	 * {@link #onOutput(Consumer)}.</p>
	 *
	 * @param errorConsumer the callback
	 * @return this command
	 */
	public Command onError(final Consumer<String> errorConsumer) {
		this.errorConsumer = Objects.requireNonNull(errorConsumer,
			"errorConsumer must not be null");

		return this;
	}

	/**
	 * Merge standard error into standard output. Every line is then delivered to
	 * {@link #onOutput(Consumer)} in the order the process wrote it, and only one reader thread is
	 * started.
	 *
	 * @param redirectErrorStream {@code true} to merge the streams
	 * @return this command
	 */
	public Command redirectErrorStream(final boolean redirectErrorStream) {
		this.redirectErrorStream = redirectErrorStream;

		return this;
	}

	/**
	 * Start the process and begin delivering its output.
	 *
	 * @return a handle on the running process, which must be {@link RunningCommand#close() closed}
	 * @throws IOException if the process cannot be started
	 */
	public RunningCommand start() throws IOException {
		final var builder = new ProcessBuilder(command).redirectErrorStream(redirectErrorStream);

		return new RunningCommand(builder.start(), charset, outputConsumer, errorConsumer,
			redirectErrorStream);
	}

	/**
	 * @return the encoding the operating system uses for a child process, falling back to
	 * {@link Charset#defaultCharset()} when {@code native.encoding} is absent or names a charset
	 * this JVM does not support
	 */
	private static Charset nativeCharset() {
		final var name = System.getProperty("native.encoding");
		if (name != null) {
			try {
				return Charset.forName(name);
			} catch (final IllegalArgumentException e) {
				// unsupported or malformed; fall through to the default
			}
		}

		return Charset.defaultCharset();
	}

}
