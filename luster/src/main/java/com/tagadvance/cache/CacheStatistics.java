package com.tagadvance.cache;

import com.google.common.base.MoreObjects;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class CacheStatistics {

	private final AtomicLong hitCount = new AtomicLong();

	private final AtomicLong missCount = new AtomicLong();

	private final AtomicLong loadSuccessCount = new AtomicLong();

	private final AtomicLong loadExceptionCount = new AtomicLong();

	private final AtomicLong loadTime = new AtomicLong();

	private final AtomicLong evictionCount = new AtomicLong();

	CacheStatistics() {

	}

	void hit() {
		hitCount.incrementAndGet();
	}

	void miss() {
		missCount.incrementAndGet();
	}

	void loadSuccess(final Duration duration) {
		final var nanos = duration.toNanos();
		loadSuccess(nanos);
	}

	void loadSuccess(final long nanos) {
		loadSuccessCount.incrementAndGet();
		loadTime.addAndGet(nanos);
	}

	void loadException() {
		loadExceptionCount.incrementAndGet();
	}

	void eviction() {
		evictionCount.incrementAndGet();
	}

	public long hitCount() {
		return hitCount.get();
	}

	public double hitRate() {
		return calculateRate(
			hitCount(),
			missCount()
		);
	}

	public long missCount() {
		return missCount.get();
	}

	public double missRate() {
		return calculateRate(
			missCount(),
			hitCount()
		);
	}

	public long loadSuccessCount() {
		return loadSuccessCount.get();
	}

	public long loadExceptionCount() {
		return loadExceptionCount.get();
	}

	public double loadSuccessRate() {
		return calculateRate(
			loadSuccessCount(),
			loadExceptionCount()
		);
	}

	public double loadExceptionRate() {
		return calculateRate(
			loadExceptionCount(),
			loadSuccessCount()
		);
	}

	public long totalLoadTime() {
		return loadTime.get();
	}

	public double averageLoadTime() {
		if (loadSuccessCount() == 0 || totalLoadTime() == 0) {
			return 0D;
		}

		return (double) loadSuccessCount() / (double) totalLoadTime();
	}

	public long totalRequestCount() {
		return hitCount.get() + loadSuccessCount.get() + loadExceptionCount.get();
	}

	public long evictionCount() {
		return evictionCount.get();
	}

	public CacheStatistics plus(final CacheStatistics... others) {
		final var stats = new CacheStatistics();
		stats.plus(this);
		Stream.of(others).forEach(this::plus);

		return stats;
	}

	private void plus(final CacheStatistics other) {
		hitCount.addAndGet(other.hitCount.get());
		missCount.addAndGet(other.missCount.get());
		loadSuccessCount.addAndGet(other.loadSuccessCount.get());
		loadExceptionCount.addAndGet(other.loadExceptionCount.get());
		loadTime.addAndGet(other.loadTime.get());
		evictionCount.addAndGet(other.evictionCount.get());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("hitCount", hitCount())
			.add("missCount", missCount())
			.add("hitRate", hitRate())
			.add("missRate", missRate())
			.add("loadSuccessCount", loadSuccessCount())
			.add("loadExceptionCount", loadExceptionCount())
			.add("loadSuccessRate", loadSuccessRate())
			.add("loadExceptionRate", loadExceptionRate())
			.add("loadTime", totalLoadTime())
			.add("evictionCount", evictionCount())
			.add("totalRequestCount", totalRequestCount())
			.toString();
	}

	/**
	 * Calculates rate from success and failure counts.
	 *
	 * @param dividend the dividend, may be zero
	 * @param divisor  the divisor, may be zero
	 * @return the quotient
	 */
	private static double calculateRate(final long dividend, final long divisor) {
		if (divisor == 0) {
			return 1D;
		} else if (dividend == 0) {
			return 0D;
		} else {
			final var total = dividend + divisor;

			return (double) dividend / total;
		}
	}

}
