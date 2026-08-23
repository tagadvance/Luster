package com.tagadvance.cache;

import com.google.common.cache.CacheStats;
import java.util.stream.Stream;

/**
 * An immutable snapshot of a {@link Cache cache's} statistics. Only populated when
 * {@link CacheConfiguration#recordStats()} is enabled; otherwise every count is zero.
 *
 * @param hitCount           the number of lookups served from the cache
 * @param missCount          the number of lookups that had to load
 * @param loadSuccessCount   the number of loads that completed successfully
 * @param loadExceptionCount the number of loads that threw
 * @param totalLoadTime      the total time spent loading, in nanoseconds
 * @param evictionCount      the number of entries evicted, not counting manual invalidation
 */
public record CacheStatistics(long hitCount, long missCount, long loadSuccessCount,
							  long loadExceptionCount, long totalLoadTime, long evictionCount) {

	static CacheStatistics from(final CacheStats stats) {
		return new CacheStatistics(stats.hitCount(), stats.missCount(), stats.loadSuccessCount(),
			stats.loadExceptionCount(), stats.totalLoadTime(), stats.evictionCount());
	}

	/**
	 * @return {@link #hitCount()} plus {@link #missCount()}
	 */
	public long totalRequestCount() {
		return hitCount + missCount;
	}

	/**
	 * @return the number of loads, successful or not
	 */
	public long loadCount() {
		return loadSuccessCount + loadExceptionCount;
	}

	/**
	 * @return the ratio of hits to requests, or {@literal 1} if there were no requests
	 */
	public double hitRate() {
		final var requestCount = totalRequestCount();

		return requestCount == 0 ? 1D : (double) hitCount / requestCount;
	}

	/**
	 * @return the ratio of misses to requests, or {@literal 0} if there were no requests
	 */
	public double missRate() {
		final var requestCount = totalRequestCount();

		return requestCount == 0 ? 0D : (double) missCount / requestCount;
	}

	/**
	 * @return the ratio of failed loads to loads, or {@literal 0} if there were no loads
	 */
	public double loadExceptionRate() {
		final var loadCount = loadCount();

		return loadCount == 0 ? 0D : (double) loadExceptionCount / loadCount;
	}

	/**
	 * @return the mean time spent loading, in nanoseconds, or {@literal 0} if there were no loads
	 */
	public double averageLoadTime() {
		final var loadCount = loadCount();

		return loadCount == 0 ? 0D : (double) totalLoadTime / loadCount;
	}

	/**
	 * @param others the snapshots to add to this one
	 * @return a new snapshot holding the element-wise sum
	 */
	public CacheStatistics plus(final CacheStatistics... others) {
		return Stream.of(others).reduce(this, CacheStatistics::plus);
	}

	private CacheStatistics plus(final CacheStatistics other) {
		return new CacheStatistics(hitCount + other.hitCount, missCount + other.missCount,
			loadSuccessCount + other.loadSuccessCount,
			loadExceptionCount + other.loadExceptionCount, totalLoadTime + other.totalLoadTime,
			evictionCount + other.evictionCount);
	}

}
