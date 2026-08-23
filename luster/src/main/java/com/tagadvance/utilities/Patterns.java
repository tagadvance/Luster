package com.tagadvance.utilities;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Patterns} caches {@link Pattern patterns} to improve performance.
 * <p>
 * If performance is critical, additional performance may be obtained by re-using a
 * {@link Matcher matcher} via {@link Matcher#reset(CharSequence)} on a per-thread basis.
 *
 * @see <a href="https://stackoverflow.com/a/19829983/625688">Answer by Seelenvirtuose</a>
 */
public class Patterns {

	@SuppressWarnings("all")
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
		return compile(regex, 0);
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
			final var key = new PatternCacheKey(regex, flags);

			return patternCache.getUnchecked(key);
		} catch (final UncheckedExecutionException e) {
			Throwables.throwIfUnchecked(e.getCause());

			throw e;
		}
	}

	private Patterns() {
	}

	private record PatternCacheKey(String regex, int flags) {

	}

}
