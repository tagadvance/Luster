package com.tagadvance.cache;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * {@link CompoundEvictionStrategy} combines multiple {@link EvictionStrategy eviction strategies}
 * into one.
 */
public final class CompoundEvictionStrategy implements EvictionStrategy {

	private final List<EvictionStrategy> strategies;

	public CompoundEvictionStrategy(final EvictionStrategy... strategies) {
		this.strategies = Arrays.asList(strategies);
		this.strategies.forEach(strategy -> requireNonNull(strategy, "strategy must not be null"));
	}

	@Override
	public void evict(final Collection<CacheEntry> entries, final int limit) {
		strategies.forEach(strategy -> strategy.evict(entries, limit));
	}

}
