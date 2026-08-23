package com.tagadvance.shell;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Unit test {@link Tail}.
 */
@Timeout(30)
class TailTest {

	private static final Path PATH = Path.of("/var/log/example.log");

	@Test
	@DisplayName("the Linux variant uses GNU long options and follows by name")
	void linux() {
		assertEquals(
			List.of("tail", "--lines=0", "--follow=name", "--retry", PATH.toString()),
			Tail.forLinux(PATH));
	}

	@Test
	@DisplayName("the BSD variant uses short options and follows by name")
	void bsd() {
		assertEquals(List.of("tail", "-n", "0", "-F", PATH.toString()), Tail.forBsd(PATH));
	}

	@Test
	@DisplayName("tail is resolved against PATH rather than hard coded")
	void relativeExecutable() {
		assertEquals("tail", Tail.forLinux(PATH).get(0));
		assertEquals("tail", Tail.forBsd(PATH).get(0));
	}

	@Test
	@DisplayName("the operating system selects the argument list")
	@EnabledOnOs(OS.LINUX)
	void dispatch() {
		assertEquals(Tail.forLinux(PATH), Tail.of(PATH));
	}

	@Test
	@DisplayName("a line appended to a followed file reaches the output callback")
	@EnabledOnOs({OS.LINUX, OS.MAC})
	void follow() throws Exception {
		final var path = Files.createTempFile("tail", ".log");
		try {
			final var latch = new CountDownLatch(1);
			final var received = new AtomicReference<String>();

			try (final var command = Command.of(Tail.of(path)).charset(UTF_8).onOutput(line -> {
				received.set(line);
				latch.countDown();
			}).start()) {
				assertTrue(command.isAlive());

				// tail gives no signal that it has opened the file, and a write that lands first
				// is simply never reported, so keep appending until a line comes back
				var delivered = false;
				for (int i = 0; i < 100 && !delivered; i++) {
					Files.writeString(path, "hello" + System.lineSeparator(), APPEND);
					delivered = latch.await(200, TimeUnit.MILLISECONDS);
				}

				assertTrue(delivered, "no line was received");
				assertEquals("hello", received.get());
			}
		} finally {
			Files.deleteIfExists(path);
		}
	}

}
