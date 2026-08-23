package com.tagadvance.cache;

import com.google.common.cache.CacheBuilder;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;

/**
 * Declares that a method's result should be cached.
 * <p>
 * Durations are ISO-8601 strings as parsed by {@link Duration#parse(CharSequence)}, e.g.
 * {@code "PT5M"} for five minutes. An empty string leaves that feature disabled. Values are
 * parsed and validated when the proxy is created, so a malformed duration fails fast rather than
 * at first use.
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheConfiguration {

	/**
	 * @return this cache's name, which must be unique within the proxied interface
	 */
	String name();

	/**
	 * @return an ISO-8601 duration, or {@literal ""} to disable
	 * @see CacheBuilder#expireAfterAccess(Duration)
	 */
	String expireAfterAccess() default "";

	/**
	 * @return an ISO-8601 duration, or {@literal ""} to disable
	 * @see CacheBuilder#expireAfterWrite(Duration)
	 */
	String expireAfterWrite() default "";

	/**
	 * @return an ISO-8601 duration, or {@literal ""} to disable
	 * @see CacheBuilder#refreshAfterWrite(Duration)
	 */
	String refreshAfterWrite() default "";

	/**
	 * @return the initial capacity
	 * @see CacheBuilder#initialCapacity(int)
	 */
	int initialCapacity() default 1;

	/**
	 * @return the maximum number of entries, or a negative value for unbounded
	 * @see CacheBuilder#maximumSize(long)
	 */
	long maximumSize() default -1L;

	/**
	 * @return {@literal true} to accumulate {@link CacheStatistics statistics}
	 * @see CacheBuilder#recordStats()
	 */
	boolean recordStats() default false;

	/**
	 * @return {@literal true} to hold values by {@link java.lang.ref.SoftReference}
	 * @see CacheBuilder#softValues()
	 */
	boolean softValues() default false;

}
