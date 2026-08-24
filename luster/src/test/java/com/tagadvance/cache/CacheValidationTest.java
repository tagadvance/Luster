package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Configuration is parsed and validated when the proxy is created, not at first use.
 */
final class CacheValidationTest {

	@Test
	void aDuplicateCacheNameIsRejected() {
		final var e = assertThrows(IllegalArgumentException.class,
			() -> new DefaultCacheFactory().newCache(DuplicateNames.class, new DuplicateNamesImpl()));

		assertTrue(e.getMessage().contains("more than one cache"), e.getMessage());
	}

	@Test
	void aMalformedDurationIsRejected() {
		final var e = assertThrows(IllegalArgumentException.class,
			() -> new DefaultCacheFactory().newCache(BadDuration.class, (BadDuration) () -> "x"));

		assertTrue(e.getMessage().contains("ISO-8601"), e.getMessage());
	}

	@Test
	void aNonPositiveDurationIsRejected() {
		final var e = assertThrows(IllegalArgumentException.class,
			() -> new DefaultCacheFactory().newCache(ZeroDuration.class, (ZeroDuration) () -> "x"));

		assertTrue(e.getMessage().contains("must be positive"), e.getMessage());
	}

	public interface DuplicateNames {

		@CacheConfiguration(name = "duplicate")
		String one();

		@CacheConfiguration(name = "duplicate")
		String two();

	}

	public static final class DuplicateNamesImpl implements DuplicateNames {

		@Override
		public String one() {
			return "one";
		}

		@Override
		public String two() {
			return "two";
		}

	}

	public interface BadDuration {

		@CacheConfiguration(name = "bad", expireAfterWrite = "5 minutes")
		String get();

	}

	public interface ZeroDuration {

		@CacheConfiguration(name = "zero", expireAfterWrite = "PT0S")
		String get();

	}

}
