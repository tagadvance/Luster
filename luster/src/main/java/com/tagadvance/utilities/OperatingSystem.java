package com.tagadvance.utilities;

import com.google.common.base.StandardSystemProperty;
import java.util.Arrays;
import java.util.Optional;

/**
 * Enum of common operating systems.
 */
public enum OperatingSystem {

	LINUX,

	MAC,

	SOLARIS,

	WINDOWS;

	private static final String OS_NAME = Optional.of(StandardSystemProperty.OS_NAME)
		.map(StandardSystemProperty::value)
		.map(String::toLowerCase)
		.orElse("");

	public static Optional<OperatingSystem> getOperatingSystem() {
		if (OperatingSystem.isLinux()) {
			return Optional.of(LINUX);
		} else if (OperatingSystem.isMac()) {
			return Optional.of(MAC);
		} else if (OperatingSystem.isSolaris()) {
			return Optional.of(SOLARIS);
		} else if (OperatingSystem.isWindows()) {
			return Optional.of(WINDOWS);
		} else {
			return Optional.empty();
		}
	}

	public static boolean isLinux() {
		return matches("linux");
	}

	public static boolean isMac() {
		return matches("darwin", "mac");
	}

	public static boolean isSolaris() {
		return matches("solaris", "sunos");
	}

	public static boolean isWindows() {
		return matches("windows");
	}

	/**
	 *
	 * @param osNames an {@link String[] array} of lowercase operating system names
	 * @return {@link Boolean#TRUE true} if any of the supplied names match the current operating
	 * system
	 */
	public static boolean matches(final String... osNames) {
		return Arrays.stream(osNames).anyMatch(OS_NAME::contains);
	}

}
