package com.tagadvance.cache;

import java.lang.ref.SoftReference;
import java.time.Instant;
import java.util.Optional;

class SoftCacheEntry implements CacheEntry {

	private final Instant creationTime;
	private final SoftReference<Object> value;

	SoftCacheEntry(final Object value) {
		this(null, value);
	}

	SoftCacheEntry(final Instant creationTime, final Object value) {
		this.creationTime = Optional.ofNullable(creationTime).orElseGet(Instant::now);
		this.value = new SoftReference<>(value);
	}

	@Override
	public Instant creationTime() {
		return creationTime;
	}

	@Override
	public Object value() {
		return value.get();
	}

}
