package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CacheStatistics}, whose arithmetic previously had no coverage at all.
 */
final class CacheStatisticsTest {

	@Test
	void totalRequestCountIsHitsPlusMisses() {
		final var stats = new CacheStatistics(3, 2, 2, 0, 100, 0);

		assertEquals(5, stats.totalRequestCount());
	}

	@Test
	void averageLoadTimeIsTimePerLoad() {
		final var stats = new CacheStatistics(0, 4, 3, 1, 800, 0);

		assertEquals(200D, stats.averageLoadTime());
	}

	@Test
	void averageLoadTimeIsZeroWithoutLoads() {
		assertEquals(0D, new CacheStatistics(1, 0, 0, 0, 0, 0).averageLoadTime());
	}

	@Test
	void plusAccumulatesEveryOperand() {
		final var one = new CacheStatistics(1, 1, 1, 1, 1, 1);

		final var sum = one.plus(one, one);

		assertEquals(new CacheStatistics(3, 3, 3, 3, 3, 3), sum);
	}

	@Test
	void plusLeavesTheReceiverUntouched() {
		final var one = new CacheStatistics(1, 1, 1, 1, 1, 1);

		one.plus(one);

		assertEquals(new CacheStatistics(1, 1, 1, 1, 1, 1), one);
	}

	@Test
	void ratesAreRelativeToRequestCount() {
		final var stats = new CacheStatistics(3, 1, 1, 0, 10, 0);

		assertEquals(0.75D, stats.hitRate());
		assertEquals(0.25D, stats.missRate());
	}

	@Test
	void ratesAreDefinedWithoutRequests() {
		final var empty = new CacheStatistics(0, 0, 0, 0, 0, 0);

		assertEquals(1D, empty.hitRate());
		assertEquals(0D, empty.missRate());
		assertEquals(0D, empty.loadExceptionRate());
	}

}
