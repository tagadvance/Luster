package com.tagadvance.proxy;

public interface Chain<I> {

	Invocation<I> invocation();

	Object proceed(Invocation<I> invocation) throws Throwable;

}
