package com.tagadvance.stack;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

import com.tagadvance.reflection.M;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The most common way to consume annotations is by using some kind of framework to search for
 * applicable components, i.e. from the top-down. This is arguably the <i>correct</i> way to consume
 * annotations. I find that there are times when one wants to modify the behavior of a component
 * depending on an annotation further up the stack, i.e. bottom-up. {@link StackAnnotations} helps
 * with the latter.
 */
public class StackAnnotations {

	private final Predicate<StackTraceElement> filter;

	/**
	 * Constructs a {@link StackAnnotations}.
	 *
	 * @param prefilter a {@link Predicate filter} that tests
	 *                  {@link StackTraceElement stack trace elements}
	 */
	public StackAnnotations(final Predicate<StackTraceElement> prefilter) {
		this.filter = requireNonNull(prefilter, "prefilter must not be null");
	}

	/**
	 * @return a {@link Stream#distinct() distinct} {@link Stream stream} of
	 * {@link Annotation annotations} pulled from the methods, classes, and packages in the
	 * {@link Thread#getStackTrace() current stack trace} with the {@link Predicate prefilter}
	 * applied
	 */
	public Stream<Annotation> getAnnotations() {
		final var list = accessorStream().skip(1).toList();

		final var packages = new LinkedHashSet<Package>();
		final var methodsAndClasses = list.stream().flatMap(a -> {
			final var method = a.getElementMethod();
			final var c = a.getElementClass();
			final var p = c.getPackage();
			packages.add(p);

			return Stream.of(method, c).flatMap(M::getAnnotations);
		});
		final var packageAnnotations = packages.stream().flatMap(M::getAnnotations);

		return Stream.of(methodsAndClasses, packageAnnotations).flatMap(identity()).distinct();
	}

	/**
	 * @return a {@link Stream stream} of {@link StackTraceElementAccessor accessors} with the
	 * {@link Predicate prefilter} applied
	 */
	public Stream<StackTraceElementAccessor> accessorStream() {
		return StackTraces.asStream().skip(1).filter(filter).map(StackTraceElementAccessor::new);
	}

	/**
	 * This is a utility class that resolves {@link StackTraceElement#getClassName() getClassName()}
	 * to its {@link Class class} and {@link StackTraceElement#getMethodName() getMethodName()} to
	 * its {@link Method method}.
	 *
	 * @param e a {@link StackTraceElement stack trace element}
	 */
	public record StackTraceElementAccessor(StackTraceElement e) {

		/**
		 * @return the {@link Class#forName(String) class} for the
		 * {@link StackTraceElement#getClassName() class name}
		 */
		public Class<?> getElementClass() {
			final var className = e.getClassName();

			try {
				return Class.forName(className);
			} catch (final ClassNotFoundException ex) {
				throw new RuntimeException("This should never happen!", ex);
			}
		}

		/**
		 * @return the {@link Method method} for the
		 * {@link StackTraceElement#getMethodName() method name}
		 */
		public Method getElementMethod() {
			return getElementMethod((methods, lineNumber) -> {
				final var list = methods.toList();
				if (list.size() == 1) {
					return list.get(0);
				}

				final var message = "Ambiguous overloaded methods: " + list.stream()
					.map(StackTraceElementAccessor::formatMethod)
					.collect(Collectors.joining(", "));
				throw new IllegalStateException(message);
			});
		}

		private static String formatMethod(final Method method) {
			final var simpleClassName = method.getClass().getSimpleName();
			final var methodName = method.getName();
			final var parameters = M.getParameters(method)
				.map(StackTraceElementAccessor::formatParameter)
				.collect(Collectors.joining(", "));

			return "%s#%s(%s)".formatted(simpleClassName, methodName, parameters);
		}

		private static String formatParameter(final Parameter parameter) {
			return parameter.getType().getSimpleName();
		}

		/**
		 * @param handler an {@link OverloadedMethodsHandler handler} to
		 *                {@link OverloadedMethodsHandler#disambiguate(Stream, int) disambiguate}
		 *                overloaded methods
		 * @return the {@link Method method} for the
		 * {@link StackTraceElement#getMethodName() method name}
		 */
		public Method getElementMethod(final OverloadedMethodsHandler handler) {
			final var methods = getElementMethods();
			final var lineNumber = e.getLineNumber();

			return handler.disambiguate(methods, lineNumber);
		}

		/**
		 * @return a {@link Stream stream} of {@link Method methods} with the
		 * {@link StackTraceElement#getMethodName() method name}
		 */
		public Stream<Method> getElementMethods() {
			final var methodName = e.getMethodName();

			return Stream.of(getElementClass())
				.flatMap(M::getMethods)
				.filter(M.withEquals(Method::getName, methodName));
		}

	}

	/**
	 * {@link OverloadedMethodsHandler}.
	 */
	@FunctionalInterface
	public interface OverloadedMethodsHandler {

		/**
		 * @param methods    a {@link Stream stream} of ambiguous/overloaded {@link Method methods}
		 * @param lineNumber the {@link StackTraceElement#getLineNumber() line number}
		 * @return the correct {@link Method methods} for the supplied {@literal lineNumber}
		 */
		Method disambiguate(Stream<Method> methods, final int lineNumber);

	}

}
