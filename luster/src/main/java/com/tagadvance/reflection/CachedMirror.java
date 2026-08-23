package com.tagadvance.reflection;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A memoized view of {@link Mirror}, obtained from {@link Mirror#cached()}. Every lookup is
 * computed once per {@link Class} and replayed on subsequent calls, which avoids the array clone
 * that {@link Class#getDeclaredMethods()} and its siblings perform on every invocation.
 *
 * <p>The cache is backed by {@link ClassValue}, so it is keyed on {@link Class} identity, is safe
 * for concurrent use, and does not pin a class loader.
 *
 * <p><strong>Entries are never invalidated.</strong> A class redefined at runtime - by an
 * instrumentation agent, or by a hot-reloading container - keeps serving the members it had when
 * it was first seen. That is why memoization is an explicit entry point rather than a silent
 * optimization of {@link Mirror}: code that must observe such a change has to call the uncached
 * methods.
 *
 * <p>Members are replayed, not copied, so a call to
 * {@link AccessibleObject#setAccessible(boolean)} on a cached member is visible to every other
 * caller of this view.
 */
public final class CachedMirror {

	static final CachedMirror INSTANCE = new CachedMirror();

	private final ClassValue<List<Constructor<?>>> constructors = memoize(
		c -> Mirror.getConstructors(c).map(constructor -> (Constructor<?>) constructor));
	private final ClassValue<List<Field>> fields = memoize(Mirror::getFields);
	private final ClassValue<List<Method>> methods = memoize(Mirror::getMethods);
	private final ClassValue<List<Class<?>>> classes = memoize(Mirror::getClasses);
	private final ClassValue<List<Class<?>>> superclasses = memoize(Mirror::getSuperclasses);
	private final ClassValue<List<Class<?>>> interfaces = memoize(Mirror::getInterfaces);
	private final ClassValue<List<Field>> allFields = memoize(Mirror::getAllFields);
	private final ClassValue<List<Method>> allMethods = memoize(Mirror::getAllMethods);
	private final ClassValue<List<RecordComponent>> recordComponents = memoize(
		Mirror::getRecordComponents);

	private CachedMirror() {
	}

	private static <T> ClassValue<List<T>> memoize(final Function<Class<?>, Stream<T>> lookup) {
		return new ClassValue<>() {

			@Override
			protected List<T> computeValue(final Class<?> type) {
				return lookup.apply(type).toList();
			}

		};
	}

	/**
	 * @param c   an instance of {@link Class}
	 * @param <T> the type of the {@link Class}
	 * @return a memoized {@link Stream stream} of all available {@link Constructor constructors}
	 * @see Mirror#getConstructors(Class)
	 */
	@SuppressWarnings("unchecked")
	public <T> Stream<Constructor<T>> getConstructors(final Class<T> c) {
		return constructors.get(c).stream().map(constructor -> (Constructor<T>) constructor);
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of all available {@link Field fields}
	 * @see Mirror#getFields(Class)
	 */
	public Stream<Field> getFields(final Class<?> c) {
		return fields.get(c).stream();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of all available {@link Method methods}
	 * @see Mirror#getMethods(Class)
	 */
	public Stream<Method> getMethods(final Class<?> c) {
		return methods.get(c).stream();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of all available {@link Class classes}
	 * @see Mirror#getClasses(Class)
	 */
	public Stream<Class<?>> getClasses(final Class<?> c) {
		return classes.get(c).stream();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of {@literal c} and its superclasses
	 * @see Mirror#getSuperclasses(Class)
	 */
	public Stream<Class<?>> getSuperclasses(final Class<?> c) {
		return superclasses.get(c).stream();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of the interfaces of {@literal c}
	 * @see Mirror#getInterfaces(Class)
	 */
	public Stream<Class<?>> getInterfaces(final Class<?> c) {
		return interfaces.get(c).stream();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of every {@link Field field} in the hierarchy
	 * @see Mirror#getAllFields(Class)
	 */
	public Stream<Field> getAllFields(final Class<?> c) {
		return allFields.get(c).stream();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of every {@link Method method} in the hierarchy
	 * @see Mirror#getAllMethods(Class)
	 */
	public Stream<Method> getAllMethods(final Class<?> c) {
		return allMethods.get(c).stream();
	}

	/**
	 * @param c an instance of {@link Class}
	 * @return a memoized {@link Stream stream} of the {@link RecordComponent record components} of
	 * {@literal c}
	 * @see Mirror#getRecordComponents(Class)
	 */
	public Stream<RecordComponent> getRecordComponents(final Class<?> c) {
		return recordComponents.get(c).stream();
	}

}
