package com.tagadvance.shell;

import java.nio.file.Path;

/**
 * Process builder for <a href="https://man7.org/linux/man-pages/man1/tail.1.html">tail</a>
 */
public final class Tail {

	public static ProcessBuilder forLinux(final Path path) {
		return new ProcessBuilder("/usr/bin/tail", "--lines=0", "--follow", path.toString());
	}

	private Tail() {
		// hide constructor
	}

}
