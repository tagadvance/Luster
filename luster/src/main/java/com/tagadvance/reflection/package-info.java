/**
 * Reflection adapted to {@link java.util.stream.Stream streams}: the array-returning lookups of
 * {@link java.lang.Class} become streams, and the checked exceptions of
 * {@link java.lang.reflect.Field} and {@link java.lang.reflect.Method} become
 * {@link com.tagadvance.reflection.ReflectionException}.
 */
@NullMarked
package com.tagadvance.reflection;

import org.jspecify.annotations.NullMarked;
