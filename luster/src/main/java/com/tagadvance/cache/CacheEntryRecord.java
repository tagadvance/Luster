package com.tagadvance.cache;

import java.time.Instant;
import java.util.Optional;

class CacheEntryRecord implements CacheEntry {

	private final Instant creationTime;
	private final Object value;

	CacheEntryRecord(final Object value) {
		this(null, value);
	}

	public CacheEntryRecord(final Instant creationTime, final Object value) {
		this.creationTime = Optional.ofNullable(creationTime).orElseGet(Instant::now);
		this.value = value;
	}

	@Override
	public Instant creationTime() {
		return creationTime;
	}

	@Override
	public Object value() {
		return value;
	}

}
