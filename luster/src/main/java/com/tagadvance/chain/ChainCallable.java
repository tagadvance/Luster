package com.tagadvance.chain;

@FunctionalInterface
public interface ChainCallable<R, E extends Throwable> {

	R call() throws E;

}
