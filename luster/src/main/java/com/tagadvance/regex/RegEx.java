package com.tagadvance.regex;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link RegEx} caches {@link Pattern patterns} to improve performance.
 * <p>
 * If performance is critical, additional performance may be obtained by re-using a
 * {@link Matcher matcher} via {@link Matcher#reset(CharSequence)} on a per-thread basis.
 *
 * @see <a href="https://stackoverflow.com/a/19829983/625688">Answer by Seelenvirtuose</a>
 */
public class RegEx {

	private static final LoadingCache<PatternCacheKey, Pattern> patternCache = CacheBuilder.newBuilder()
		.expireAfterAccess(1, TimeUnit.MINUTES)
		.build(new CacheLoader<>() {
			@Override
			public Pattern load(final PatternCacheKey key) {
				return Pattern.compile(key.regex, key.flags);
			}
		});

	/**
	 * The {@link Pattern pattern} returned by this method is cached until one minute after the last
	 * time it is accessed.
	 *
	 * @param regex a regular expression
	 * @return a {@link Pattern pattern}
	 * @see Pattern#compile(String)
	 */
	public static Pattern compile(final String regex) {
		try {
			return patternCache.get(new PatternCacheKey(regex, 0));
		} catch (final ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The {@link Pattern pattern} returned by this method is cached until one minute after the last
	 * time it is accessed.
	 *
	 * @param regex a regular expression
	 * @param flags a bit mask that may include {@link Pattern#CASE_INSENSITIVE},
	 *              {@link Pattern#MULTILINE}, {@link Pattern#DOTALL}, etc...
	 * @return a {@link Pattern pattern}
	 * @see Pattern#compile(String, int)
	 */
	public static Pattern compile(final String regex, final int flags) {
		try {
			return patternCache.get(new PatternCacheKey(regex, flags));
		} catch (final ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private RegEx() {
	}

	private record PatternCacheKey(String regex, int flags) {

		@Override
		public boolean equals(final Object o) {
			return o instanceof final PatternCacheKey that && flags == that.flags && Objects.equals(
				regex, that.regex);
		}

		@Override
		public int hashCode() {
			return Objects.hash(regex, flags);
		}

	}

}
