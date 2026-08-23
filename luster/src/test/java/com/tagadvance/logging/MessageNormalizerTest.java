package com.tagadvance.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MessageNormalizer}.
 */
class MessageNormalizerTest {

	private final java.util.function.UnaryOperator<String> normalizer =
		MessageNormalizer.collapsingValues();

	@Test
	void anSlf4jTemplateIsUnchanged() {
		assertEquals("Failed to sync tenant {}", normalizer.apply("Failed to sync tenant {}"));
	}

	@Test
	void integersCollapse() {
		assertEquals(normalizer.apply("Failed to sync tenant 41"),
			normalizer.apply("Failed to sync tenant 1523"));
	}

	@Test
	void decimalsAndNegativesCollapse() {
		assertEquals("took # seconds", normalizer.apply("took 1.25 seconds"));
		assertEquals("offset #", normalizer.apply("offset -7"));
	}

	@Test
	void uuidsCollapse() {
		assertEquals(normalizer.apply("tenant 3f2504e0-4f89-11d3-9a0c-0305e82c3301"),
			normalizer.apply("tenant 7c9e6679-7425-40de-944b-e07fc1f90ae7"));
	}

	@Test
	void timestampsCollapse() {
		assertEquals(normalizer.apply("expired at 2026-07-26T14:02:11Z"),
			normalizer.apply("expired at 2026-07-27T09:41:03Z"));
	}

	@Test
	void wordsAreLeftAlone() {
		assertEquals("connection refused", normalizer.apply("connection refused"));
	}

	@Test
	void shortHexIsTreatedAsAWord() {
		// "cafe" is a word before it is a hex run
		assertEquals("cafe", normalizer.apply("cafe"));
	}

}
