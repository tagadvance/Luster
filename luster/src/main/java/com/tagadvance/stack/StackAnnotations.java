package com.tagadvance.stack;

import static java.util.Objects.requireNonNull;

import com.tagadvance.reflection.M;
import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The most common way to consume annotations is by using some kind of framework to search for
 * applicable components, i.e. from the top-down. This is arguably the <i>correct</i> way to consume
 * annotations. I find that there are times when one wants to modify the behavior of a component
 * depending on an annotation further up the stack, i.e. bottom-up. {@link StackAnnotations} helps
 * with the latter.
 * <p>
 * Only the annotations present on the method, its declaring class, and that class's package are
 * collected. Meta-annotations — annotations on those annotations — are deliberately not followed;
 * the point is to read what a caller declared, not to reason about an annotation hierarchy.
 * {@link Inherited @Inherited} annotations do come along, because
 * {@link Class#getAnnotations() Class#getAnnotations()} reports them, but only through
 * superclasses; interfaces never contribute.
 */
public final class StackAnnotations {

	private final Predicate<StackFrame> filter;

	/**
	 * Constructs a {@link StackAnnotations}.
	 *
	 * @param prefilter a {@link Predicate filter} that tests {@link StackFrame stack frames}
	 */
	public StackAnnotations(final Predicate<StackFrame> prefilter) {
		this.filter = requireNonNull(prefilter, "prefilter must not be null");
	}

	/**
	 * Frames belonging to {@link StackAnnotations} and {@link StackTraces} are removed before the
	 * {@link Predicate prefilter} is applied, so the first frame seen is always the caller,
	 * regardless of how many intermediate methods this class grows.
	 *
	 * @return a {@link Stream#distinct() distinct} {@link Stream stream} of
	 * {@link Annotation annotations} pulled from the methods, classes, and packages in the current
	 * stack with the {@link Predicate prefilter} applied
	 */
	public Stream<Annotation> getAnnotations() {
		final var accessors = accessorStream().toList();

		final var packages = new LinkedHashSet<Package>();
		final var methodsAndClasses = accessors.stream().flatMap(accessor -> {
			final var c = accessor.getElementClass();
			packages.add(c.getPackage());

			return Stream.concat(accessor.findElementMethod().stream(), Stream.of(c))
				.flatMap(M::getAnnotations);
		});
		final var packageAnnotations = packages.stream().flatMap(M::getAnnotations);

		return Stream.of(methodsAndClasses, packageAnnotations)
			.flatMap(Function.identity())
			.distinct();
	}

	/**
	 * @return a {@link Stream stream} of {@link StackFrameAccessor accessors} with the
	 * {@link Predicate prefilter} applied
	 */
	public Stream<StackFrameAccessor> accessorStream() {
		return StackTraces.asStream()
			.filter(StackTraces.remove(StackAnnotations.class))
			.filter(filter)
			.map(StackFrameAccessor::new);
	}

	/**
	 * This is a utility class that resolves a {@link StackFrame stack frame} to its
	 * {@link Class class} and {@link Method method}.
	 *
	 * @param frame a {@link StackFrame stack frame}
	 */
	public record StackFrameAccessor(StackFrame frame) {

		/**
		 * Unlike a {@link Class#forName(String) name lookup}, this cannot fail: the JVM already
		 * holds the reference. That matters for the frames a name lookup chokes on — lambda proxy
		 * classes, hidden classes, and anything loaded by a class loader other than the current
		 * one.
		 *
		 * @return the {@link StackFrame#getDeclaringClass() declaring class} of the frame
		 */
		public Class<?> getElementClass() {
			return frame.getDeclaringClass();
		}

		/**
		 * Resolves the frame to exactly one {@link Method method} using its
		 * {@link StackFrame#getMethodType() method type}, so overloads are not ambiguous.
		 * <p>
		 * The result is empty for a frame that has no {@link Method method} at all — a
		 * constructor, a static initializer, or a synthetic frame the declaring class does not
		 * declare.
		 *
		 * @return the {@link Method method} the frame represents
		 */
		public Optional<Method> findElementMethod() {
			final var methodType = frame.getMethodType();

			try {
				return Optional.of(getElementClass().getDeclaredMethod(frame.getMethodName(),
					methodType.parameterArray()));
			} catch (final NoSuchMethodException e) {
				return Optional.empty();
			}
		}

		/**
		 * @return the {@link Method method} the frame represents
		 * @throws IllegalStateException if the frame does not represent a {@link Method method}
		 * @see #findElementMethod()
		 */
		public Method getElementMethod() {
			return findElementMethod().orElseThrow(() -> new IllegalStateException(
				"%s does not declare a method matching %s".formatted(getElementClass().getName(),
					formatFrame())));
		}

		/**
		 * An escape hatch for the caller who wants to pick the method itself.
		 * {@link #findElementMethod()} is the default and does not need it.
		 *
		 * @param handler an {@link OverloadedMethodsHandler handler} to
		 *                {@link OverloadedMethodsHandler#disambiguate(Stream, int) disambiguate}
		 *                overloaded methods
		 * @return the {@link Method method} chosen by {@literal handler}
		 */
		public Method getElementMethod(final OverloadedMethodsHandler handler) {
			return handler.disambiguate(getElementMethods(), frame.getLineNumber());
		}

		/**
		 * @return a {@link Stream stream} of {@link Method methods} with the
		 * {@link StackFrame#getMethodName() method name}, ignoring the signature
		 */
		public Stream<Method> getElementMethods() {
			final var methodName = frame.getMethodName();

			return Stream.of(getElementClass())
				.flatMap(M::getMethods)
				.filter(M.withEquals(Method::getName, methodName));
		}

		private String formatFrame() {
			final var parameters = frame.getMethodType()
				.parameterList()
				.stream()
				.map(Class::getSimpleName)
				.collect(Collectors.joining(", "));

			return "%s(%s)".formatted(frame.getMethodName(), parameters);
		}

	}

	/**
	 * {@link OverloadedMethodsHandler}.
	 */
	@FunctionalInterface
	public interface OverloadedMethodsHandler {

		/**
		 * @param methods    a {@link Stream stream} of ambiguous/overloaded {@link Method methods}
		 * @param lineNumber the {@link StackFrame#getLineNumber() line number}
		 * @return the correct {@link Method methods} for the supplied {@literal lineNumber}
		 */
		Method disambiguate(Stream<Method> methods, final int lineNumber);

	}

}
