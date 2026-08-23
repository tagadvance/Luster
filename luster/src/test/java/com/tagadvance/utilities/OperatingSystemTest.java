package com.tagadvance.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OperatingSystemTest {

	@Test
	@DisplayName("Linux os.name strings match \"linux\"")
	void matchesLinux() {
		assertTrue(OperatingSystem.matchesOsName("Linux", "linux"));
	}

	@Test
	@DisplayName("macOS os.name strings match \"darwin\" or \"mac\"")
	void matchesMac() {
		assertTrue(OperatingSystem.matchesOsName("Mac OS X", "darwin", "mac"));
		assertTrue(OperatingSystem.matchesOsName("Darwin", "darwin", "mac"));
	}

	@Test
	@DisplayName("Solaris os.name strings match \"solaris\" or \"sunos\"")
	void matchesSolaris() {
		assertTrue(OperatingSystem.matchesOsName("Solaris", "solaris", "sunos"));
		assertTrue(OperatingSystem.matchesOsName("SunOS", "solaris", "sunos"));
	}

	@Test
	@DisplayName("Windows os.name strings match \"windows\"")
	void matchesWindows() {
		assertTrue(OperatingSystem.matchesOsName("Windows 11", "windows"));
	}

	@Test
	@DisplayName("FreeBSD os.name strings match \"freebsd\"")
	void matchesFreeBsd() {
		assertTrue(OperatingSystem.matchesOsName("FreeBSD", "freebsd"));
	}

	@Test
	@DisplayName("AIX os.name strings match \"aix\"")
	void matchesAix() {
		assertTrue(OperatingSystem.matchesOsName("AIX", "aix"));
	}

	@Test
	@DisplayName("an unrecognized os.name matches none of the known names")
	void unknownSystemDoesNotMatch() {
		assertFalse(OperatingSystem.matchesOsName("OS/2 Warp", "linux", "mac", "windows"));
	}

	@Test
	@DisplayName("matching is case-insensitive on both the os.name and the supplied names")
	void matchingIsCaseInsensitive() {
		assertTrue(OperatingSystem.matchesOsName("LINUX", "linux"));
		assertTrue(OperatingSystem.matchesOsName("linux", "LINUX"));
	}

}
