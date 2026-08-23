package com.tagadvance.reflection;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.function.Predicate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mirror Hierarchy Test")
class MirrorHierarchyTest {

	@Test
	@DisplayName("getSuperclasses() walks from the class itself up to Object")
	void getSuperclasses() {
		final var superclasses = M.getSuperclasses(Child.class).toList();

		assertEquals(List.of(Child.class, Parent.class, Object.class), superclasses);
	}

	@Test
	@DisplayName("getSuperclasses() of Object yields Object")
	void getSuperclassesOfObject() {
		assertEquals(List.of(Object.class), M.getSuperclasses(Object.class).toList());
	}

	@Test
	@DisplayName("getInterfaces() collects super-interfaces transitively")
	void getInterfaces() {
		final var interfaces = M.getInterfaces(Child.class).toList();

		assertEquals(List.of(Middle.class, Base.class), interfaces);
	}

	@Test
	@DisplayName("getFields() cannot see a non-public inherited field")
	void getFieldsIsAHalfWalk() {
		final var names = M.getFields(Child.class).map(Field::getName).toList();

		assertEquals(List.of("childPublic"), names);
	}

	@Test
	@DisplayName("getAllFields() sees non-public fields declared by a superclass")
	void getAllFields() {
		final var names = M.getAllFields(Child.class)
			.map(Field::getName)
			.collect(Collectors.toSet());

		assertEquals(Set.of("childPublic", "parentSecret", "parentPackagePrivate"), names);
	}

	@Test
	@DisplayName("getAllMethods() yields an overridden method once per declaring class")
	void getAllMethods() {
		final var declaringClasses = M.getAllMethods(Child.class)
			.filter(M.withEquals(Method::getName, "describe"))
			.map(Method::getDeclaringClass)
			.toList();

		assertEquals(List.of(Child.class, Parent.class), declaringClasses);
	}

	@Test
	@DisplayName("getMethods() yields the bridge method generated for a generic override")
	void bridgeAndSynthetic() {
		final var compareTo = M.getMethods(Sortable.class)
			.filter(M.withEquals(Method::getName, "compareTo"))
			.toList();

		assertEquals(2, compareTo.size());

		final var bridges = compareTo.stream().filter(M::isBridge).toList();

		assertEquals(1, bridges.size());
		assertEquals(Object.class, bridges.get(0).getParameterTypes()[0]);
		assertTrue(M.isSynthetic(bridges.get(0)));

		final var declared = compareTo.stream().filter(Predicate.not(M::isBridge)).toList();

		assertEquals(1, declared.size());
		assertEquals(Sortable.class, declared.get(0).getParameterTypes()[0]);
	}

	@Test
	@DisplayName("getRecordComponents() yields components in declaration order")
	void getRecordComponents() {
		final var names = M.getRecordComponents(Point.class)
			.map(RecordComponent::getName)
			.toList();

		assertEquals(List.of("x", "y"), names);
	}

	@Test
	@DisplayName("getRecordComponents() of a class that is not a record is empty")
	void getRecordComponentsOfNonRecord() {
		assertEquals(0, M.getRecordComponents(Child.class).count());
	}

	@Test
	@DisplayName("getParameterAnnotations() flattens the per-parameter arrays")
	void getParameterAnnotations() {
		final var annotationTypes = M.getMethods(Child.class)
			.filter(M.withEquals(Method::getName, "annotated"))
			.flatMap(M::getParameterAnnotations)
			.map(Annotation::annotationType)
			.toList();

		assertEquals(List.of(Marked.class), annotationTypes);
	}

	@Test
	@DisplayName("get() rethrows IllegalAccessException as ReflectionException")
	void getRethrows() {
		final var parent = new Parent();
		final var field = M.getAllFields(Parent.class)
			.filter(M.withEquals(Field::getName, "parentSecret"))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("parentSecret is required by this test"));

		final var e = assertThrows(ReflectionException.class, () -> M.get(parent).apply(field));

		assertInstanceOf(IllegalAccessException.class, e.getCause());
	}

	@Test
	@DisplayName("invoke() rethrows the target exception as ReflectionException")
	void invokeRethrows() {
		final var child = new Child();
		final var method = M.getMethods(Child.class)
			.filter(M.withEquals(Method::getName, "boom"))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("boom is required by this test"));

		final var e = assertThrows(ReflectionException.class, () -> M.invoke(child).apply(method));
		final var cause = assertInstanceOf(InvocationTargetException.class, e.getCause());

		assertInstanceOf(UnsupportedOperationException.class, cause.getCause());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@interface Marked {

	}

	interface Base {

	}

	interface Middle extends Base {

	}

	static class Parent implements Middle {

		private final String parentSecret = "parent";
		static final int parentPackagePrivate = 1;

		String describe() {
			return parentSecret;
		}

	}

	static class Child extends Parent {

		public final String childPublic = "child";

		@Override
		String describe() {
			return childPublic;
		}

		void annotated(@Marked final String a, final int b) {
		}

		void boom() {
			throw new UnsupportedOperationException("boom");
		}

	}

	static class Sortable implements Comparable<Sortable> {

		@Override
		public int compareTo(final Sortable o) {
			return 0;
		}

	}

	record Point(int x, int y) {

	}

}
