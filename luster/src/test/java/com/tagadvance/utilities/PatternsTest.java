package com.tagadvance.utilities;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PatternsTest {

	@Test
	@DisplayName("compiling the same regex and flags returns the same cached Pattern instance")
	void sameRegexReturnsSameInstance() {
		final var a = Patterns.compile("abc", Pattern.CASE_INSENSITIVE);
		final var b = Patterns.compile("abc", Pattern.CASE_INSENSITIVE);

		assertSame(a, b);
	}

	@Test
	@DisplayName("the same regex with different flags produces different Pattern instances")
	void differentFlagsReturnDifferentInstances() {
		final var a = Patterns.compile("abc", 0);
		final var b = Patterns.compile("abc", Pattern.CASE_INSENSITIVE);

		assertNotSame(a, b);
	}

	@Test
	@DisplayName("an invalid regex surfaces as PatternSyntaxException, not a wrapper exception")
	void invalidRegexThrowsPatternSyntaxException() {
		assertThrows(PatternSyntaxException.class, () -> Patterns.compile("["));
	}

	@Test
	@DisplayName("the cache does not grow past its configured maximum size")
	void cacheIsBoundedByMaximumSize() {
		for (var i = 0; i < Patterns.MAXIMUM_CACHE_SIZE * 2; i++) {
			Patterns.compile("regex-" + i);
		}

		assertTrue(Patterns.estimatedCacheSize() <= Patterns.MAXIMUM_CACHE_SIZE);
	}

}
