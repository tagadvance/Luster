package com.tagadvance.chain;

@FunctionalInterface
public interface ChainLink<R, E extends Throwable> {

	R next(final Chain<R, E> chain) throws E;

}
