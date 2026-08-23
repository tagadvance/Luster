package com.tagadvance.shell;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Unit test {@link Command} and {@link RunningCommand}. Every test drives a POSIX shell, so the
 * suite is limited to platforms that have one.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@Timeout(30)
class CommandTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	@Test
	@DisplayName("a process that exits ends its callbacks and releases its reader threads")
	void exit() throws Exception {
		final var lines = lines();

		try (final var command = Command.of("sh", "-c", "printf 'a\\nb\\n'")
			.charset(UTF_8)
			.onOutput(lines::add)
			.start()) {
			assertEquals(0, command.awaitExit(TIMEOUT));
			assertEquals(List.of("a", "b"), lines);
			assertFalse(command.isAlive());

			// the bug this API replaces: readers that never observed end of file were polled
			// forever, so the worker thread never went idle
			assertEquals(0, readerThreads(command.pid()));
		}
	}

	@Test
	@DisplayName("a non-zero exit code is reported")
	void failure() throws Exception {
		try (final var command = Command.of("sh", "-c", "exit 3").start()) {
			assertEquals(3, command.awaitExit(TIMEOUT));
		}
	}

	@Test
	@DisplayName("standard output and standard error are routed to separate callbacks")
	void split() throws Exception {
		final var out = lines();
		final var err = lines();

		try (final var command = Command.of("sh", "-c", "printf 'out\\n'; printf 'err\\n' >&2")
			.charset(UTF_8)
			.onOutput(out::add)
			.onError(err::add)
			.start()) {
			assertEquals(0, command.awaitExit(TIMEOUT));
			assertEquals(List.of("out"), out);
			assertEquals(List.of("err"), err);
		}
	}

	@Test
	@DisplayName("redirectErrorStream delivers standard error to the output callback")
	void merged() throws Exception {
		final var out = lines();
		final var err = lines();

		try (final var command = Command.of("sh", "-c", "printf 'err\\n' >&2; printf 'out\\n'")
			.charset(UTF_8)
			.redirectErrorStream(true)
			.onOutput(out::add)
			.onError(err::add)
			.start()) {
			assertEquals(0, command.awaitExit(TIMEOUT));
			assertEquals(List.of("err", "out"), out);
			assertTrue(err.isEmpty());
		}
	}

	@Test
	@DisplayName("output is decoded with the configured charset")
	void charset() throws Exception {
		final var utf8 = lines();
		final var latin1 = lines();

		// octal escapes so the bytes on the wire do not depend on how the JVM encodes arguments
		final var command = Command.of("sh", "-c", "printf 'caf\\303\\251\\n'");

		try (final var running = command.charset(UTF_8).onOutput(utf8::add).start()) {
			assertEquals(0, running.awaitExit(TIMEOUT));
		}

		try (final var running = command.charset(ISO_8859_1).onOutput(latin1::add).start()) {
			assertEquals(0, running.awaitExit(TIMEOUT));
		}

		assertEquals(List.of("caf\u00e9"), utf8);
		assertEquals(List.of("caf\u00c3\u00a9"), latin1);
	}

	@Test
	@DisplayName("close terminates a process that is still running")
	void closeTerminates() throws Exception {
		final var command = Command.of("sleep", "30").start();
		try {
			assertTrue(command.isAlive());
		} finally {
			command.close();
		}

		assertFalse(command.isAlive());
		assertEquals(0, readerThreads(command.pid()));
	}

	@Test
	@DisplayName("awaitExit times out while the process is still running")
	void awaitExitTimesOut() throws Exception {
		try (final var command = Command.of("sleep", "30").start()) {
			assertThrows(TimeoutException.class,
				() -> command.awaitExit(Duration.ofMillis(100)));
		}
	}

	@Test
	@DisplayName("the child sees end of file on standard input")
	void stdinIsClosed() throws Exception {
		final var lines = lines();

		try (final var command = Command.of("cat").onOutput(lines::add).start()) {
			assertEquals(0, command.awaitExit(TIMEOUT));
			assertTrue(lines.isEmpty());
		}
	}

	@Test
	@DisplayName("an empty command is rejected")
	void empty() {
		assertThrows(IllegalArgumentException.class, () -> Command.of(List.of()));
	}

	@Test
	@DisplayName("a missing executable fails to start")
	void missingExecutable() {
		assertThrows(IOException.class,
			() -> Command.of("this-executable-does-not-exist-4f2a").start());
	}

	private static List<String> lines() {
		return Collections.synchronizedList(new ArrayList<>());
	}

	private static long readerThreads(final long pid) {
		final var prefix = "%s-%d-".formatted(RunningCommand.THREAD_NAME_PREFIX, pid);

		return Thread.getAllStackTraces()
			.keySet()
			.stream()
			.filter(Thread::isAlive)
			.filter(thread -> thread.getName().startsWith(prefix))
			.count();
	}

}
