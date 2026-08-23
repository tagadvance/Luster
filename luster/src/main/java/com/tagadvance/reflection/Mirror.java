package com.tagadvance.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * {@link Mirror} is a utility to simplify reflection through the use of {@link Stream streams}.
 *
 * <p>This class is deliberately not {@literal final}: {@link M} extends it to provide a short
 * alias. Do not "fix" it.
 */
public class Mirror {

	/**
	 * Returns a memoized view of this utility. Lookups like {@link Class#getDeclaredMethods()}
	 * clone their backing array on every call, so a {@link Stream stream} built over a hot class
	 * allocates on every pass; the returned view computes each lookup once per {@link Class} and
	 * replays it.
	 *
	 * <p>Memoization is opt-in rather than transparent because the cache is never invalidated. A
	 * class redefined at runtime - by an instrumentation agent, or by a hot-reloading container -
	 * keeps serving the members it had when it was first seen. Callers that must observe such a
	 * change have to use the uncached methods.
	 *
	 * @return a shared, memoized view of this utility
	 * @see CachedMirror
	 */
	public static CachedMirror cached() {
		return CachedMirror.INSTANCE;
	}

	/**
	 * Note that {@link AccessibleObject#canAccess(Object)} throws
	 * {@link IllegalArgumentException} rather than returning {@literal false} when
	 * {@literal instance} is {@literal null} and the object under test is an instance member, or
	 * when {@literal instance} is not an instance of the declaring class. Use
	 * {@link #canAccessStatic()} to test static access.
	 *
	 * @param instance an {@link Object object}
	 * @return a {@link Predicate filter} that retains {@link AccessibleObject accessible objects}
	 * that can be accessed
	 * @see AccessibleObject#canAccess(Object)
	 */
	public static Predicate<AccessibleObject> canAccess(final @Nullable Object instance) {
		return accessibleObject -> accessibleObject.canAccess(instance);
	}

	/**
	 * @return a {@link Predicate filter} that retains {@link AccessibleObject accessible objects}
	 * that can be accessed statically. Instance members are rejected rather than tested, so this
	 * {@link Predicate filter} is safe to apply to a {@link Stream stream} that mixes static and
	 * instance members.
	 * @see #canAccess(Object)
	 */
	public static Predicate<AccessibleObject> canAccessStatic() {
		// canAccess(null) throws IllegalArgumentException for an instance member instead of
		// returning false, so instance members must never reach it.
		return accessibleObject -> {
			final var isInstanceMember = accessibleObject instanceof Member member
				&& !(accessibleObject instanceof Constructor<?>) && !isStatic(member);

			return !isInstanceMember && accessibleObject.canAccess(null);
		};
	}

	/**
	 * @param c   an annotation class
	 * @param <A> the type of annotation
	 * @return a {@link Predicate filter} that retains {@link AnnotatedElement elements} that have
	 * an {@link Annotation} that matches the input type
	 * @see AnnotatedElement#getDeclaredAnnotations()
	 * @see AnnotatedElement#getAnnotations()
	 */
	public static <A extends Annotation> Predicate<AnnotatedElement> hasAnnotation(
		final Class<A> c) {
		return e -> getAnnotations(e).anyMatch(c::isInstance);
	}

	/**
	 * @param e an instance of {@link AnnotatedElement}
	 * @return a {@link Stream stream} of all available annotations present on the supplied
	 * {@link AnnotatedElement element}
	 * @see AnnotatedElement#getDeclaredAnnotations()
	 * @see AnnotatedElement#getAnnotations()
	 */
	public static Stream<Annotation> getAnnotations(final AnnotatedElement e) {
		final var declaredAnnotations = e.getDeclaredAnnotations();
		final var annotations = e.getAnnotations();

		return Stream.of(declaredAnnotations, annotations).flatMap(Arrays::stream).distinct();
	}

	/**
	 * @param declaration an instance of {@link GenericDeclaration}
	 * @return a {@link Stream stream} of all available {@link TypeVariable type parameters}
	 * @see GenericDeclaration#getTypeParameters()
	 */
	public static Stream<TypeVariable<?>> getTypeParameters(final GenericDeclaration declaration) {
		final var typeParameters = declaration.getTypeParameters();

		return Arrays.stream(typeParameters);
	}

	/**
	 * @param typeVariable a {@link TypeVariable type variable}
	 * @param <D>          the type of generic declaration that declared the underlying type
	 *                     variable
	 * @return a {@link Stream stream} of {@link Type types}
	 * @see TypeVariable#getBounds()
	 */
	public static <D extends GenericDeclaration> Stream<Type> getBounds(
		final TypeVariable<D> typeVariable) {
		final var bounds = typeVariable.getBounds();

		return Arrays.stream(bounds);
	}

	/**
	 * @param typeVariable a {@link TypeVariable type variable}
	 * @param <D>          the type of generic declaration that declared the underlying type
	 *                     variable
	 * @return a {@link Stream stream} of {@link AnnotatedType annotated types}
	 * @see TypeVariable#getAnnotatedBounds()
	 */
	public static <D extends GenericDeclaration> Stream<AnnotatedType> getAnnotatedBounds(
		final TypeVariable<D> typeVariable) {
		final var annotatedBounds = typeVariable.getAnnotatedBounds();

		return Arrays.stream(annotatedBounds);
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of {@link Class parameter types}
	 * @see Executable#getParameterTypes()
	 */
	public static Stream<Class<?>> getParameterTypes(final Executable executable) {
		final var parameterTypes = executable.getParameterTypes();

		return Arrays.stream(parameterTypes);
	}

	/**
	 * @param count the expected count
	 * @return a {@link Predicate filter} that retains {@link Executable executables} with a
	 * {@link Executable#getParameterCount() parameter count} that matches the supplied
	 * {@literal count}
	 * @see Executable#getParameterCount()
	 */
	public static Predicate<Executable> hasParameterCount(final int count) {
		return executable -> executable.getParameterCount() == count;
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of {@link Type types}
	 * @see Executable#getGenericParameterTypes()
	 */
	public static Stream<Type> getGenericParameterTypes(final Executable executable) {
		final var genericParameterTypes = executable.getGenericParameterTypes();

		return Stream.of(genericParameterTypes);
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of {@link Parameter parameters}
	 * @see Executable#getParameters()
	 */
	public static Stream<Parameter> getParameters(final Executable executable) {
		final var parameters = executable.getParameters();

		return Stream.of(parameters);
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of {@link Class exception types}
	 * @see Executable#getExceptionTypes()
	 */
	public static Stream<Class<?>> getExceptionTypes(final Executable executable) {
		final var exceptionTypes = executable.getExceptionTypes();

		return Stream.of(exceptionTypes);
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of {@link Type generic exception types}
	 * @see Executable#getGenericExceptionTypes()
	 */
	public static Stream<Type> getGenericExceptionTypes(final Executable executable) {
		final var genericExceptionTypes = executable.getGenericExceptionTypes();

		return Stream.of(genericExceptionTypes);
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of the {@link Annotation annotations} declared on the
	 * {@link Executable executable's} parameters, flattened into a single {@link Stream stream} in
	 * parameter order
	 * @see Executable#getParameterAnnotations()
	 */
	public static Stream<Annotation> getParameterAnnotations(final Executable executable) {
		final var parameterAnnotations = executable.getParameterAnnotations();

		return Arrays.stream(parameterAnnotations).flatMap(Arrays::stream);
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of {@link AnnotatedType annotated parameter types}
	 * @see Executable#getAnnotatedParameterTypes()
	 */
	public static Stream<AnnotatedType> getAnnotatedParameterTypes(final Executable executable) {
		final var annotatedParameterTypes = executable.getAnnotatedParameterTypes();

		return Stream.of(annotatedParameterTypes);
	}

	/**
	 * @param executable an instance of {@link Executable}
	 * @return a {@link Stream stream} of {@link AnnotatedType annotated parameter types}
	 * @see Executable#getAnnotatedExceptionTypes()
	 */
	public static Stream<AnnotatedType> getAnnotatedExceptionTypes(final Executable executable) {
		final var annotatedExceptionTypes = executable.getAnnotatedExceptionTypes();

		return Stream.of(annotatedExceptionTypes);
	}

	/**
	 * Constructors are not inherited, so this is the union of two overlapping sets: every
	 * constructor declared by {@literal c}, and the {@literal public} subset of the same.
	 *
	 * @param c   an instance of {@link Class}
	 * @param <T> the type of the {@link Class}
	 * @return a {@link Stream stream} of all available {@link Constructor constructors}
	 * @see Class#getDeclaredConstructors()
	 * @see Class#getConstructors()
	 */
	@SuppressWarnings("unchecked")
	public static <T> Stream<Constructor<T>> getConstructors(final Class<T> c) {
		final var declaredConstructors = (Constructor<T>[]) c.getDeclaredConstructors();
		final var constructors = (Constructor<T>[]) c.getConstructors();

		return Stream.of(declaredConstructors, constructors).flatMap(Arrays::stream).distinct();
	}

	/**
	 * This is the union of the fields <em>declared</em> by {@literal c} and the
	 * {@literal public} fields of its whole hierarchy, which is neither "declared" nor "all": a
	 * non-public field inherited from a superclass is invisible. Use
	 * {@link #getAllFields(Class)} to walk the hierarchy instead.
	 *
	 * <p>{@link Stream#distinct()} leans on {@link Field#equals(Object)}, which includes the
	 * declaring class, so a field that shadows an inherited one appears once per declaring class.
	 *
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of all available {@link Field fields}
	 * @see Class#getDeclaredFields()
	 * @see Class#getFields()
	 */
	public static Stream<Field> getFields(final Class<?> c) {
		final var declaredFields = c.getDeclaredFields();
		final var fields = c.getFields();

		return Stream.of(declaredFields, fields).flatMap(Arrays::stream).distinct();
	}

	/**
	 * @param instance an {@link Object object}
	 * @param <R>      the return type
	 * @return a curried {@link Function mapper function} that returns the result of a call to
	 * {@link Field#get(Object)}  with the supplied input
	 * @throws ReflectionException if an {@link IllegalAccessException} is caught it'll be re-thrown
	 *                             as a {@link ReflectionException}
	 * @see Field#get(Object)
	 */
	@SuppressWarnings("unchecked")
	public static <R> Function<Field, R> get(final @Nullable Object instance) {
		return field -> {
			try {
				return (R) field.get(instance);
			} catch (final IllegalAccessException e) {
				throw new ReflectionException(e);
			}
		};
	}

	/**
	 * @param <R> the return type
	 * @return a curried {@link Function mapper function} that returns the result of a call to
	 * {@link Field#get(Object)} with a {@literal null} value
	 * @see #get(Object)
	 */
	public static <R> Function<Field, R> getStatic() {
		return get(null);
	}

	/**
	 * This is the union of the methods <em>declared</em> by {@literal c} and the
	 * {@literal public} methods of its whole hierarchy, which is neither "declared" nor "all": a
	 * non-public method inherited from a superclass is invisible. Use
	 * {@link #getAllMethods(Class)} to walk the hierarchy instead.
	 *
	 * <p>Bridge and synthetic methods come through. Every generic override produces a bridge
	 * method, so a {@link Stream stream} over a concrete {@link Comparable} implementation yields
	 * two {@literal compareTo} entries with different parameter types. Filter them with
	 * {@link #isBridge(Member)} or {@link #isSynthetic(Member)}.
	 *
	 * <p>{@link Stream#distinct()} leans on {@link Method#equals(Object)}, which includes the
	 * declaring class, so an overridden method appears once per class in the hierarchy that
	 * declares it.
	 *
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of all available {@link Method methods}
	 * @see Class#getDeclaredMethods()
	 * @see Class#getMethods()
	 */
	public static Stream<Method> getMethods(final Class<?> c) {
		final var declaredMethods = c.getDeclaredMethods();
		final var methods = c.getMethods();

		return Stream.of(declaredMethods, methods).flatMap(Arrays::stream).distinct();
	}

	/**
	 * @param instance  an {@link Object object}
	 * @param arguments TODO
	 * @param <R>       the return type
	 * @return a curried {@link Function mapper function} that returns the result of a call to
	 * {@link Method#invoke(Object, Object...)}  with the supplied input
	 * @throws ReflectionException if an {@link IllegalAccessException} or
	 *                             {@link InvocationTargetException} is caught it'll be re-thrown as
	 *                             a {@link ReflectionException}
	 * @see Method#invoke(Object, Object...)
	 */
	@SuppressWarnings("unchecked")
	public static <R> Function<Method, R> invoke(final @Nullable Object instance,
		final @Nullable Object... arguments) {
		return method -> {
			try {
				return (R) method.invoke(instance, arguments);
			} catch (final IllegalAccessException | InvocationTargetException e) {
				throw new ReflectionException(e);
			}
		};
	}

	/**
	 * @param arguments TODO
	 * @param <R>       the return type
	 * @return a curried {@link Function mapper function} that returns the result of a call to
	 * {@link Method#invoke(Object, Object...)} with a {@literal null} value and the supplied
	 * arguments
	 * @see #invoke(Object, Object...)
	 */
	public static <R> Function<Method, R> invokeStatic(final @Nullable Object... arguments) {
		return invoke(null, arguments);
	}

	/**
	 * This is the union of the member classes <em>declared</em> by {@literal c} and the
	 * {@literal public} member classes of its whole hierarchy: a non-public member class
	 * inherited from a superclass is invisible.
	 *
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of all available {@link Class classes}
	 * @see Class#getDeclaredClasses()
	 * @see Class#getClasses()
	 */
	public static Stream<Class<?>> getClasses(final Class<?> c) {
		final var declaredClasses = c.getDeclaredClasses();
		final var classes = c.getClasses();

		return Stream.of(declaredClasses, classes).flatMap(Arrays::stream).distinct();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of {@literal c} followed by each of its superclasses,
	 * ending with {@link Object}; an interface, a primitive and {@link Object} itself each yield a
	 * single element
	 * @see Class#getSuperclass()
	 */
	public static Stream<Class<?>> getSuperclasses(final Class<?> c) {
		return Stream.iterate(c, Objects::nonNull, Class::getSuperclass);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of every interface implemented by {@literal c} or by one of
	 * its superclasses, transitively
	 * @see Class#getInterfaces()
	 * @see #getSuperclasses(Class)
	 */
	public static Stream<Class<?>> getInterfaces(final Class<?> c) {
		return getSuperclasses(c).map(Class::getInterfaces)
			.flatMap(Arrays::stream)
			.flatMap(i -> Stream.concat(Stream.of(i), getInterfaces(i)))
			.distinct();
	}

	/**
	 * Unlike {@link #getFields(Class)} this walks the whole hierarchy, so a non-public inherited
	 * field is visible. A field that shadows an inherited one appears once per declaring class.
	 *
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of every {@link Field field} declared by {@literal c}, by
	 * one of its superclasses, or by one of its interfaces, whatever its visibility
	 * @see Class#getDeclaredFields()
	 * @see #getSuperclasses(Class)
	 * @see #getInterfaces(Class)
	 */
	public static Stream<Field> getAllFields(final Class<?> c) {
		return Stream.concat(getSuperclasses(c), getInterfaces(c))
			.map(Class::getDeclaredFields)
			.flatMap(Arrays::stream)
			.distinct();
	}

	/**
	 * Unlike {@link #getMethods(Class)} this walks the whole hierarchy, so a non-public inherited
	 * method is visible. An overridden method appears once per class in the hierarchy that
	 * declares it.
	 *
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of every {@link Method method} declared by {@literal c}, by
	 * one of its superclasses, or by one of its interfaces, whatever its visibility
	 * @see Class#getDeclaredMethods()
	 * @see #getSuperclasses(Class)
	 * @see #getInterfaces(Class)
	 */
	public static Stream<Method> getAllMethods(final Class<?> c) {
		return Stream.concat(getSuperclasses(c), getInterfaces(c))
			.map(Class::getDeclaredMethods)
			.flatMap(Arrays::stream)
			.distinct();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a {@link Stream stream} of the {@link RecordComponent record components} of
	 * {@literal c} in declaration order, or an empty {@link Stream stream} if {@literal c} is not
	 * a record
	 * @see Class#getRecordComponents()
	 */
	public static Stream<RecordComponent> getRecordComponents(final Class<?> c) {
		final var recordComponents = c.getRecordComponents();

		return recordComponents == null ? Stream.empty() : Arrays.stream(recordComponents);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return {@literal true} if the supplied {@link Class class} is {@literal public}
	 * @see Modifier#isPublic(int)
	 */
	public static boolean isPublic(final Class<?> c) {
		final var modifiers = c.getModifiers();

		return Modifier.isPublic(modifiers);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return {@literal true} if the supplied {@link Class class} is {@literal private}
	 * @see Modifier#isPrivate(int)
	 */
	public static boolean isPrivate(final Class<?> c) {
		final var modifiers = c.getModifiers();

		return Modifier.isPrivate(modifiers);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return {@literal true} if the supplied {@link Class class} is {@literal protected}
	 * @see Modifier#isProtected(int)
	 */
	public static boolean isProtected(final Class<?> c) {
		final var modifiers = c.getModifiers();

		return Modifier.isProtected(modifiers);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return {@literal true} if the supplied {@link Class class} is {@literal static}
	 * @see Modifier#isStatic(int)
	 */
	public static boolean isStatic(final Class<?> c) {
		final var modifiers = c.getModifiers();

		return Modifier.isStatic(modifiers);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return {@literal true} if the supplied {@link Class class} is {@literal final}
	 * @see Modifier#isFinal(int)
	 */
	public static boolean isFinal(final Class<?> c) {
		final var modifiers = c.getModifiers();

		return Modifier.isFinal(modifiers);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return {@literal true} if the supplied {@link Class class} is an {@literal interface}
	 * @see Modifier#isInterface(int)
	 */
	public static boolean isInterface(final Class<?> c) {
		final var modifiers = c.getModifiers();

		return Modifier.isInterface(modifiers);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return {@literal true} if the supplied {@link Class class} is {@literal abstract}
	 * @see Modifier#isAbstract(int)
	 */
	public static boolean isAbstract(final Class<?> c) {
		final var modifiers = c.getModifiers();

		return Modifier.isAbstract(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal public}
	 * @see Modifier#isPublic(int)
	 */
	public static boolean isPublic(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isPublic(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal private}
	 * @see Modifier#isPrivate(int)
	 */
	public static boolean isPrivate(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isPrivate(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal protected}
	 * @see Modifier#isProtected(int)
	 */
	public static boolean isProtected(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isProtected(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal static}
	 * @see Modifier#isStatic(int)
	 */
	public static boolean isStatic(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isStatic(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal final}
	 * @see Modifier#isFinal(int)
	 */
	public static boolean isFinal(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isFinal(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal synchronized}
	 * @see Modifier#isSynchronized(int)
	 */
	public static boolean isSynchronized(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isSynchronized(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal volatile}
	 * @see Modifier#isVolatile(int)
	 */
	public static boolean isVolatile(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isVolatile(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal transient}
	 * @see Modifier#isTransient(int)
	 */
	public static boolean isTransient(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isTransient(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is {@literal native}
	 * @see Modifier#isNative(int)
	 */
	public static boolean isNative(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isNative(modifiers);
	}

	/**
	 * @param member an instance of {@link Member}
	 * @return {@literal true} if the supplied {@link Member member} is abstract
	 * @see Modifier#isAbstract(int)
	 */
	public static boolean isAbstract(final Member member) {
		final var modifiers = member.getModifiers();

		return Modifier.isAbstract(modifiers);
	}

	/**
	 * @param member a {@link Member member}
	 * @return {@literal true} if {@literal member} is a bridge {@link Method method}, i.e. one of
	 * the synthetic overloads the compiler generates so that a generic override is reachable
	 * through its erased signature
	 * @see Method#isBridge()
	 */
	public static boolean isBridge(final Member member) {
		return member instanceof final Method method && method.isBridge();
	}

	/**
	 * @param member a {@link Member member}
	 * @return {@literal true} if {@literal member} was introduced by the compiler rather than
	 * declared in source
	 * @see Member#isSynthetic()
	 */
	public static boolean isSynthetic(final Member member) {
		return member.isSynthetic();
	}

	/**
	 * @param mapper e.g. {@literal Class<?>::getName}
	 * @param value  e.g. {@literal "Foo"}
	 * @param <T>    the input type
	 * @param <R>    the type of the mapped value
	 * @return a {@link Predicate filter} that tests the value returned by the
	 * {@link Function mapper function} using {@link Objects#equals(Object, Object)}
	 */
	public static <T, R> Predicate<T> withEquals(final Function<T, R> mapper, final R value) {
		return with(mapper, result -> Objects.equals(result, value));
	}

	/**
	 * @param mapper e.g. {@literal Class<?>::getName}
	 * @param filter e.g. {@literal "Foo"::equals}
	 * @param <T>    the input type
	 * @param <R>    the type of the mapped value
	 * @return a {@link Predicate filter} that tests the value returned by the
	 * {@link Function mapper function}
	 */
	public static <T, R> Predicate<T> with(final Function<T, R> mapper, final Predicate<R> filter) {
		return entity -> {
			final var value = mapper.apply(entity);

			return filter.test(value);
		};
	}

	/**
	 * @param mapper e.g. {@literal Method::getParameters}
	 * @param values an array to compare to the mapped value
	 * @param <T>    the input type
	 * @param <R>    the type of the mapped value
	 * @return a {@link Predicate filter} that tests the value returned by the
	 * {@link Function mapper function} using {@link Arrays#equals(Object[], Object[])}
	 */
	@SafeVarargs
	public static <T, R> Predicate<T> withArrayEquals(final Function<T, R[]> mapper,
		final R... values) {
		return with(mapper, arr -> Arrays.equals(arr, values));
	}

	Mirror() {
	}

}
