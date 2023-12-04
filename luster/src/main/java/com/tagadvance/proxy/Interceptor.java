package com.tagadvance.proxy;

@FunctionalInterface
public interface Interceptor<I> {

	Object intercept(final Chain<I> chain) throws Throwable;

}
