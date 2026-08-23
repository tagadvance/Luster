package com.tagadvance.shell;

import com.tagadvance.utilities.OperatingSystem;
import java.nio.file.Path;
import java.util.List;

/**
 * Argument lists for <a href="https://man7.org/linux/man-pages/man1/tail.1.html">tail</a>, suitable
 * for {@link Command#of(List)}.
 *
 * <p>Every variant follows by <em>name</em> rather than by descriptor. Plain {@code --follow}
 * tracks the open inode, so when logrotate renames the file out from under it the tailer keeps
 * reading a file nothing writes to any more and silently goes quiet. Following by name with retry
 * reopens the path instead.</p>
 *
 * <p>{@code tail} is resolved against {@code PATH} rather than hard coded to {@code /usr/bin/tail},
 * which is not where it lives on NixOS, in a slimmed down container image, or when Homebrew's
 * coreutils shadow the system copy.</p>
 */
public final class Tail {

	/**
	 * Follow a file, starting at its end, using the flags of the current operating system.
	 *
	 * @param path the file to follow
	 * @return the command and its arguments
	 * @throws UnsupportedOperationException on Windows, which has no {@code tail}
	 */
	public static List<String> of(final Path path) {
		return OperatingSystem.getOperatingSystem()
			.map(operatingSystem -> switch (operatingSystem) {
				case WINDOWS ->
					throw new UnsupportedOperationException("tail is not available on Windows");
				case LINUX -> forLinux(path);
				// GNU long options exist only on Linux; the short forms are the safer guess
				default -> forBsd(path);
			})
			.orElseGet(() -> forBsd(path));
	}

	/**
	 * Follow a file, starting at its end, using GNU coreutils long options.
	 *
	 * @param path the file to follow
	 * @return the command and its arguments
	 */
	public static List<String> forLinux(final Path path) {
		return List.of("tail", "--lines=0", "--follow=name", "--retry", path.toString());
	}

	/**
	 * Follow a file, starting at its end, using the short options understood by BSD and macOS
	 * {@code tail}.
	 *
	 * @param path the file to follow
	 * @return the command and its arguments
	 */
	public static List<String> forBsd(final Path path) {
		return List.of("tail", "-n", "0", "-F", path.toString());
	}

	private Tail() {
		// hide constructor
	}

}
