package com.tagadvance.chain;

public interface Chain<R, E extends Throwable> {

	ChainCallable<R, E> input();

	R next(ChainCallable<R, E> callable) throws E;

}
