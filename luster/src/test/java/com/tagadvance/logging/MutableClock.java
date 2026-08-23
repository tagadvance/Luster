package com.tagadvance.logging;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A {@link Clock} the tests move by hand, so summary durations are deterministic.
 */
final class MutableClock extends Clock {

	private Instant instant = Instant.parse("2026-07-26T14:00:00Z");

	void advance(final Duration duration) {
		instant = instant.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return ZoneId.of("UTC");
	}

	@Override
	public Clock withZone(final ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return instant;
	}

}
