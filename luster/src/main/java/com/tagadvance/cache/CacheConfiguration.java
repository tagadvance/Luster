package com.tagadvance.cache;

import com.google.common.cache.CacheBuilder;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * The cache configuration.
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheConfiguration {

	String name();

	/**
	 * See Also: {@link CacheBuilder#expireAfterAccess(long, TimeUnit)}
	 */
	long expireAfterAccessDelay() default -1L;

	/**
	 * See Also: {@link CacheBuilder#expireAfterAccess(long, TimeUnit)}
	 */
	TimeUnit expireAfterAccessTimeUnit() default TimeUnit.MILLISECONDS;

	/**
	 * See Also: {@link CacheBuilder#expireAfterWrite(long, TimeUnit)}
	 */
	long expireAfterWriteDelay() default -1L;

	/**
	 * See Also: {@link CacheBuilder#expireAfterWrite(long, TimeUnit)}
	 */
	TimeUnit expireAfterWriteTimeUnit() default TimeUnit.MILLISECONDS;

	/**
	 * The name of the method to use to calculate the expiration from the result. Must accept one
	 * argument and return a {@link java.time.Duration} or {@link Long millis}.
	 * <p>
	 * .e.g. <code>"com.domain.Class#methodName"</code> or simply <code>"methodName"</code> if it
	 * belongs to the same interface.
	 *
	 * @return the name of the method to use to calculate the expiration from the result
	 */
	String expireAfterWriteHook() default "";

	/**
	 * See Also: {@link CacheBuilder#initialCapacity(int)}
	 */
	int initialCapacity() default 1;

	/**
	 * See Also: {@link CacheBuilder#maximumSize(long)}
	 */
	long maximumSize() default -1L;

	/**
	 * See Also: {@link CacheBuilder#maximumWeight(long)}
	 */
	long maximumWeight() default -1L;

	/**
	 * See Also: {@link CacheBuilder#refreshAfterWrite(long, TimeUnit)}
	 */
	boolean recordStats() default false;

	/**
	 * See Also: {@link CacheBuilder#refreshAfterWrite(long, TimeUnit)}
	 */
	long refreshAfterWriteDelay() default -1L;

	/**
	 * See Also: {@link CacheBuilder#refreshAfterWrite(long, TimeUnit)}
	 */
	TimeUnit refreshAfterWriteTimeUnit() default TimeUnit.MILLISECONDS;

	/**
	 * See Also: {@link CacheBuilder#softValues()}
	 */
	boolean softValues() default false;

	// FIXME:
	String weigher() default "";

}
