package com.tagadvance.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test {@link Exec}.
 */
class ExecTest {

	private static final Logger logger = LoggerFactory.getLogger(ExecTest.class);

	@Test
	@Timeout(1)
	void testThatPartialLinesDoNotDeadlock() throws IOException, InterruptedException {
		final var lines = new ArrayList<String>();
		final Consumer<String> lineConsumer = lines::add;

		try (final var exec = new Exec()) {
			final var test1 = Files.createTempFile("test1", ".log");
			final var test2 = Files.createTempFile("test1", ".log");

			exec.start(Tail.forLinux(test1)::start, lineConsumer, logger::error);
			exec.start(Tail.forLinux(test2)::start, lineConsumer, logger::error);

			Thread.sleep(100);

			// ensure that
			try (final var writer1 = Files.newBufferedWriter(test1,
				StandardOpenOption.DELETE_ON_CLOSE)) {
				writer1.write("This line does not end with a newline.");

				Thread.sleep(100);

				try (final var writer2 = Files.newBufferedWriter(test2,
					StandardOpenOption.DELETE_ON_CLOSE)) {
					writer2.write("This line ends with a newline." + System.lineSeparator());
				}

				Thread.sleep(100);

				writer1.write(System.lineSeparator());
			}

			Thread.sleep(100);
		}

		assertEquals(2, lines.size());
		assertEquals("This line ends with a newline.", lines.get(0));
		assertEquals("This line does not end with a newline.", lines.get(1));
	}

}
