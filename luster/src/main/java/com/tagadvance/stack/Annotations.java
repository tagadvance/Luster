package com.tagadvance.stack;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

/**
 * {@link Annotations}
 */
public interface Annotations {

	/**
	 * @return a {@link Stream stream} of {@link Annotation annotations}
	 */
	Stream<Annotation> getAnnotations();

}
