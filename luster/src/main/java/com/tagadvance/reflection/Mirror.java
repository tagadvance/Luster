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
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@link Mirror} is a utility to simply reflection through the use of {@link Stream streams}.
 */
public class Mirror {

	/**
	 * @param instance an {@link Object object}
	 * @return a {@link Predicate filter} that retains {@link AccessibleObject accessible objects}
	 * that can be accessed
	 * @see AccessibleObject#canAccess(Object)
	 */
	public static Predicate<AccessibleObject> canAccess(final Object instance) {
		return accessibleObject -> accessibleObject.canAccess(instance);
	}

	/**
	 * @return a {@link Predicate filter} that retains {@link AccessibleObject accessible objects}
	 * that can be accessed statically
	 * @see #canAccess(Object)
	 */
	public static Predicate<AccessibleObject> canAccessStatic() {
		return canAccess(null);
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

	// TODO getParameterAnnotations

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
	public static <R> Function<Field, R> get(final Object instance) {
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
	public static <R> Function<Method, R> invoke(final Object instance, final Object... arguments) {
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
	public static <R> Function<Method, R> invokeStatic(final Object... arguments) {
		return invoke(null, arguments);
	}

	/**
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
