package com.tagadvance.chain;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

// TODO: document recommend synchronization
public class ReusableChain<R, E extends Throwable> implements Chain<R, E> {

	private final Deque<ChainCallable<R, E>> callbacks = new LinkedList<>();
	private final Deque<ChainLink<R, E>> links;

	public ReusableChain(final ChainLink<R, E>... links) {
		this(Arrays.asList(links));
	}

	public ReusableChain(final List<? extends ChainLink<R, E>> links) {
		this.links = new LinkedList<>(links);
	}

	@Override
	public final ChainCallable<R, E> input() {
		return callbacks.peek();
	}

	@Override
	public final R next(final ChainCallable<R, E> callable) throws E {
		if (links.isEmpty()) {
			return callable.call();
		}

		callbacks.push(callable);
		try {
			final var link = links.removeFirst();
			try {
				return link.next(this);
			} finally {
				links.addFirst(link);
			}
		} finally {
			callbacks.pop();
		}
	}

}
