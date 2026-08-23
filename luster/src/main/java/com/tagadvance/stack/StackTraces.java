package com.tagadvance.stack;

import com.google.common.base.Throwables;
import com.tagadvance.utilities.Patterns;
import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * {@link StackTraces} contains convenience methods for walking the current call stack as a
 * {@link Stream stream} of {@link StackFrame stack frames}.
 * <p>
 * Frames are produced by a {@link StackWalker} configured with
 * {@link Option#RETAIN_CLASS_REFERENCE}, which buys three things over
 * {@link Thread#getStackTrace()}: frames are materialized lazily, in batches, so a short-circuiting
 * terminal operation never pays for the whole stack;
 * {@link StackFrame#getDeclaringClass() getDeclaringClass()} hands back the {@link Class class} the
 * JVM already had rather than requiring a fallible {@link Class#forName(String) name lookup}; and
 * {@link StackFrame#getMethodType() getMethodType()} exposes the exact method descriptor, which is
 * what makes overload resolution possible.
 */
public final class StackTraces {

	/**
	 * A {@link Pattern pattern} matching the <a
	 * href="https://docs.oracle.com/en/java/javase/17/docs/api/allpackages-index.html">JDK
	 * packages</a>, including the {@literal sun}, {@literal com.oracle}, and {@literal jrt}
	 * namespaces that are not part of the documented API but do show up in a call stack. It is
	 * exposed so that callers can see exactly what {@link #asStream()} discards, and reuse it.
	 * <p>
	 * Like every pattern accepted by {@link #retain(Pattern)}, this one is unanchored at the tail
	 * but anchored at the head, so it matches a package prefix and nothing else.
	 */
	public static final Pattern JDK_PACKAGES = Pattern.compile(
		"^(com\\.oracle|com\\.sun|java|javax|jdk|jrt|org\\.w3c\\.dom|org\\.xml\\.sax|sun)\\.");

	private static final Pattern JUNIT_PACKAGES = Pattern.compile("^org\\.junit\\.");

	private static final StackWalker WALKER = StackWalker.getInstance(
		Option.RETAIN_CLASS_REFERENCE);

	/**
	 * Applies the supplied {@link Function function} to a lazily populated {@link Stream stream} of
	 * the {@link StackFrame stack frames} of the current thread.
	 * <p>
	 * This is the primitive that the rest of this class is built on. Prefer it over
	 * {@link #asStream()} whenever the terminal operation short-circuits — {@code findFirst},
	 * {@code anyMatch}, {@code limit} — because the walk then stops as soon as the answer is known.
	 * <p>
	 * The {@link Stream stream} is closed when {@literal function} returns and must not escape it.
	 * The {@link StackFrame frames} themselves remain valid. The frames of this class are removed,
	 * so the first frame is always the caller.
	 *
	 * @param function a {@link Function function} that consumes the
	 *                 {@link StackFrame stack frames}
	 * @param <T>      the type of the result
	 * @return the result of {@literal function}
	 * @see StackWalker#walk(Function)
	 */
	public static <T> T walk(final Function<? super Stream<StackFrame>, ? extends T> function) {
		return WALKER.walk(stream -> function.apply(stream.filter(remove(StackTraces.class))));
	}

	/**
	 * The returned {@link Stream stream} is detached from the walk, so it may be stored or returned
	 * — at the cost of eagerly collecting the frames that survive the filters. Use
	 * {@link #walk(Function)} where that cost matters.
	 *
	 * @return the {@link StackFrame stack frames} of the current thread as a {@link Stream stream}
	 * with the frames of this class and of the {@link #JDK_PACKAGES JDK packages} removed
	 */
	public static Stream<StackFrame> asStream() {
		return walk(stream -> stream.filter(remove(JDK_PACKAGES)).toList()).stream();
	}

	/**
	 * An alias of {@link #retain(Class)}.{@link Predicate#negate() negate()}.
	 *
	 * @param clazz a {@link Class class}
	 * @return a {@link Predicate filter} that removes {@link StackFrame stack frames} declared by
	 * {@literal clazz}
	 */
	public static Predicate<StackFrame> remove(final Class<?> clazz) {
		return retain(clazz).negate();
	}

	/**
	 * An alias of {@link #retain(String)}.{@link Predicate#negate() negate()}.
	 *
	 * @param classNameRegex a regular expression
	 * @return a {@link Predicate filter} that removes {@link StackFrame stack frames} whose
	 * {@link StackFrame#getClassName() class name} matches {@literal classNameRegex}
	 */
	public static Predicate<StackFrame> remove(final String classNameRegex) {
		return retain(classNameRegex).negate();
	}

	/**
	 * An alias of {@link #retain(Pattern)}.{@link Predicate#negate() negate()}.
	 *
	 * @param pattern a {@link Pattern pattern}
	 * @return a {@link Predicate filter} that removes {@link StackFrame stack frames} whose
	 * {@link StackFrame#getClassName() class name} matches {@literal pattern}
	 */
	public static Predicate<StackFrame> remove(final Pattern pattern) {
		return retain(pattern).negate();
	}

	/**
	 * Frames are compared by {@link StackFrame#getDeclaringClass() declaring class} rather than by
	 * name, so two identically named classes loaded by different class loaders are not conflated.
	 *
	 * @param clazz a {@link Class class}
	 * @return a {@link Predicate filter} that retains {@link StackFrame stack frames} declared by
	 * {@literal clazz}
	 */
	public static Predicate<StackFrame> retain(final Class<?> clazz) {
		return frame -> frame.getDeclaringClass().equals(clazz);
	}

	/**
	 * The pattern is matched with {@link java.util.regex.Matcher#find() find()}, not
	 * {@link java.util.regex.Matcher#matches() matches()}, so it is <b>unanchored</b>: it need only
	 * occur somewhere within the class name. {@literal com\.tagadvance} therefore also retains
	 * {@literal org.evil.com.tagadvance.Thing}. Anchor it — {@literal ^com\.tagadvance\.} — when
	 * that matters.
	 *
	 * @param classNameRegex a regular expression
	 * @return a {@link Predicate filter} that retains {@link StackFrame stack frames} whose
	 * {@link StackFrame#getClassName() class name} matches {@literal classNameRegex}
	 * @see Patterns#compile(String)
	 */
	public static Predicate<StackFrame> retain(final String classNameRegex) {
		final var pattern = Patterns.compile(classNameRegex);

		return retain(pattern);
	}

	/**
	 * The pattern is matched with {@link java.util.regex.Matcher#find() find()}, not
	 * {@link java.util.regex.Matcher#matches() matches()}, so it is <b>unanchored</b>: it need only
	 * occur somewhere within the class name. {@literal com\.tagadvance} therefore also retains
	 * {@literal org.evil.com.tagadvance.Thing}. Anchor it — {@literal ^com\.tagadvance\.} — when
	 * that matters.
	 *
	 * @param pattern a {@link Pattern pattern}
	 * @return a {@link Predicate filter} that retains {@link StackFrame stack frames} whose
	 * {@link StackFrame#getClassName() class name} matches {@literal pattern}
	 */
	public static Predicate<StackFrame> retain(final Pattern pattern) {
		return frame -> pattern.matcher(frame.getClassName()).find();
	}

	/**
	 * @param pattern a {@link Pattern pattern}
	 * @return a {@link Predicate filter} that retains {@link StackTraceElement stack trace
	 * elements} whose {@link StackTraceElement#getClassName() class name} matches
	 * {@literal pattern}
	 * @see #retain(Pattern)
	 */
	public static Predicate<StackTraceElement> retainElement(final Pattern pattern) {
		return element -> pattern.matcher(element.getClassName()).find();
	}

	/**
	 * Prunes a copy of the {@link Throwable#getStackTrace() stack trace} of {@literal throwable}.
	 * The {@link Throwable throwable} is not modified.
	 *
	 * @param throwable a {@link Throwable throwable}
	 * @param pattern   a {@link Pattern pattern}; {@link StackTraceElement elements} whose
	 *                  {@link StackTraceElement#getClassName() class name} does not match are
	 *                  removed
	 * @return the pruned {@link StackTraceElement stack trace}
	 */
	public static StackTraceElement[] prune(final Throwable throwable, final Pattern pattern) {
		return prune(throwable, retainElement(pattern));
	}

	/**
	 * Prunes a copy of the {@link Throwable#getStackTrace() stack trace} of {@literal throwable}.
	 * The {@link Throwable throwable} is not modified.
	 *
	 * @param throwable a {@link Throwable throwable}
	 * @param filter    a {@link Predicate filter} that retains
	 *                  {@link StackTraceElement stack trace elements}
	 * @return the pruned {@link StackTraceElement stack trace}
	 */
	public static StackTraceElement[] prune(final Throwable throwable,
		final Predicate<StackTraceElement> filter) {
		return Stream.of(throwable.getStackTrace())
			.filter(filter)
			.toArray(StackTraceElement[]::new);
	}

	/**
	 * Overwrites the {@link Throwable#setStackTrace(StackTraceElement[]) stack trace} of
	 * {@literal throwable} and of every {@link Throwable throwable} in its
	 * {@link Throwables#getCausalChain(Throwable) causal chain} with a pruned copy.
	 * <p>
	 * <b>This mutates live throwables.</b> Pruning an exception that is subsequently rethrown means
	 * whatever catches it further up sees a stack trace with the discarded frames already gone,
	 * which is difficult to diagnose from the outside. Prefer {@link #prune(Throwable, Pattern)}
	 * and use the pruned copy at the point of presentation — a log message, say — unless the
	 * {@link Throwable throwable} is genuinely owned by the caller.
	 *
	 * @param throwable a {@link Throwable throwable}
	 * @param pattern   a {@link Pattern pattern}; {@link StackTraceElement elements} whose
	 *                  {@link StackTraceElement#getClassName() class name} does not match are
	 *                  removed
	 */
	public static void pruneInPlace(final Throwable throwable, final Pattern pattern) {
		pruneInPlace(throwable, retainElement(pattern));
	}

	/**
	 * Overwrites the {@link Throwable#setStackTrace(StackTraceElement[]) stack trace} of
	 * {@literal throwable} and of every {@link Throwable throwable} in its
	 * {@link Throwables#getCausalChain(Throwable) causal chain} with a pruned copy.
	 * <p>
	 * <b>This mutates live throwables.</b> See {@link #pruneInPlace(Throwable, Pattern)}.
	 *
	 * @param throwable a {@link Throwable throwable}
	 * @param filter    a {@link Predicate filter} that retains
	 *                  {@link StackTraceElement stack trace elements}
	 */
	public static void pruneInPlace(final Throwable throwable,
		final Predicate<StackTraceElement> filter) {
		Throwables.getCausalChain(throwable)
			.forEach(t -> t.setStackTrace(prune(t, filter)));
	}

	/**
	 * Walks the stack until it finds a JUnit frame, so the common case — production code, no JUnit
	 * anywhere — costs a full walk, and the test case stops early.
	 * <p>
	 * <b>Branching on this is a debugging hazard.</b> Code that behaves one way under test and
	 * another in production is code whose tests do not exercise what actually ships, and the
	 * divergence is invisible at the call site. Reach for it for diagnostics — louder logging,
	 * an extra assertion — and not for behavior the caller depends on.
	 *
	 * @return {@literal true} if this code is being called, however indirectly, from JUnit
	 */
	public static boolean isTesting() {
		return walk(stream -> stream.anyMatch(retain(JUNIT_PACKAGES)));
	}

	private StackTraces() {
	}

}
