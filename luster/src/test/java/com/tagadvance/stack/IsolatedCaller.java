package com.tagadvance.stack;

/**
 * A test helper that is loaded by two class loaders at once. It deliberately references nothing
 * outside {@literal java.lang} so that a {@link ClassLoader class loader} with no parent can define
 * it.
 */
final class IsolatedCaller implements Runnable {

	private final Runnable delegate;

	IsolatedCaller(final Runnable delegate) {
		this.delegate = delegate;
	}

	@Override
	public void run() {
		delegate.run();
	}

}
