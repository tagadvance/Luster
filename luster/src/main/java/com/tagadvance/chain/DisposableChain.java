package com.tagadvance.chain;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class DisposableChain<R, E extends Throwable> implements Chain<R, E> {

	private final Deque<ChainLink<R, E>> links;
	private ChainCallable<R, E> callback;

	public DisposableChain(final ChainLink<R, E>... links) {
		this(Arrays.asList(links));
	}

	public DisposableChain(final List<? extends ChainLink<R, E>> links) {
		this.links = new LinkedList<>(links);
	}

	@Override
	public final ChainCallable<R, E> input() {
		return callback;
	}

	@Override
	public final R next(final ChainCallable<R, E> callable) throws E {
		if (links.isEmpty()) {
			return callable.call();
		}

		callback = callable;
		try {
			return links.removeFirst().next(this);
		} finally {
			callback = null;
		}
	}

}
