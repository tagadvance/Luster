package com.tagadvance.cache;

import java.time.Instant;

public interface CacheEntry {

	Instant creationTime();

	Object value();

}
