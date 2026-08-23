package com.tagadvance.utilities;

import com.google.common.base.StandardSystemProperty;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Enum of common operating systems.
 */
public enum OperatingSystem {

	/**
	 * Any Linux distribution.
	 */
	LINUX,

	/**
	 * macOS, which reports itself as {@literal darwin} or {@literal mac}.
	 */
	MAC,

	/**
	 * Solaris, which reports itself as {@literal solaris} or {@literal sunos}.
	 */
	SOLARIS,

	/**
	 * Any version of Windows.
	 */
	WINDOWS,

	/**
	 * FreeBSD.
	 */
	FREE_BSD,

	/**
	 * IBM AIX.
	 */
	AIX;

	private static final String OS_NAME = Optional.of(StandardSystemProperty.OS_NAME)
		.map(StandardSystemProperty::value)
		.map(name -> name.toLowerCase(Locale.ROOT))
		.orElse("");

	/**
	 * @return the {@link OperatingSystem} this JVM is running on, or {@link Optional#empty()} if
	 * it is not one this enum knows about
	 */
	public static Optional<OperatingSystem> getOperatingSystem() {
		if (OperatingSystem.isLinux()) {
			return Optional.of(LINUX);
		} else if (OperatingSystem.isMac()) {
			return Optional.of(MAC);
		} else if (OperatingSystem.isSolaris()) {
			return Optional.of(SOLARIS);
		} else if (OperatingSystem.isWindows()) {
			return Optional.of(WINDOWS);
		} else if (OperatingSystem.isFreeBsd()) {
			return Optional.of(FREE_BSD);
		} else if (OperatingSystem.isAix()) {
			return Optional.of(AIX);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * @return {@literal true} if this JVM is running on Linux
	 */
	public static boolean isLinux() {
		return matches("linux");
	}

	/**
	 * @return {@literal true} if this JVM is running on macOS
	 */
	public static boolean isMac() {
		return matches("darwin", "mac");
	}

	/**
	 * @return {@literal true} if this JVM is running on Solaris
	 */
	public static boolean isSolaris() {
		return matches("solaris", "sunos");
	}

	/**
	 * @return {@literal true} if this JVM is running on Windows
	 */
	public static boolean isWindows() {
		return matches("windows");
	}

	/**
	 * @return {@literal true} if this JVM is running on FreeBSD
	 */
	public static boolean isFreeBsd() {
		return matches("freebsd");
	}

	/**
	 * @return {@literal true} if this JVM is running on AIX
	 */
	public static boolean isAix() {
		return matches("aix");
	}

	/**
	 * @param osNames operating system names to match against the current {@code os.name}; case
	 *                is irrelevant
	 * @return {@link Boolean#TRUE true} if any of the supplied names match the current operating
	 * system
	 */
	public static boolean matches(final String... osNames) {
		return matchesOsName(OS_NAME, osNames);
	}

	/**
	 * Test seam for {@link #matches(String...)} that matches against an arbitrary {@code osName}
	 * instead of the current {@code os.name} system property.
	 *
	 * @param osName the operating system name to match against
	 * @param names  operating system names to match against {@code osName}; case is irrelevant
	 * @return {@link Boolean#TRUE true} if any of the supplied names match {@code osName}
	 */
	static boolean matchesOsName(final String osName, final String... names) {
		// Locale.ROOT, because the default locale would lowercase "LINUX" to "lınux" under tr_TR
		final var lowerOsName = osName.toLowerCase(Locale.ROOT);

		return Arrays.stream(names)
			.map(name -> name.toLowerCase(Locale.ROOT))
			.anyMatch(lowerOsName::contains);
	}

}
