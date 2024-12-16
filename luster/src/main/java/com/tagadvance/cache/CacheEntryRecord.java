package com.tagadvance.cache;

import java.time.Instant;
import java.util.Optional;

public record CacheEntryRecord(Instant creationTime, Object value) implements CacheEntry {

	public CacheEntryRecord(final Object value) {
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
