package com.tagadvance.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mirror Test")
class MirrorTest {

	Object object = new Object();
	public Object publicObject = new Object();
	private Object privateObject = new Object();
	protected Object protectedObject = new Object();
	static Object staticObject = new Object();
	volatile Object volatileObject = new Object();
	transient Object transientObject = new Object();

	@Test
	void canAccess() {
		final var canAccessMethods = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(((Predicate<? super Method>) M::isStatic).negate())
			.filter(M.canAccess(this))
			.toList();

		assertTrue(canAccessMethods.size() > 50);
	}

	@Test
	void canAccessStatic() {
		final var canAccessMethods = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(M::isStatic)
			.filter(M.canAccessStatic())
			.toList();

		assertEquals(1, canAccessMethods.size());
	}

	static void canAccessStaticFoo() {
	}

	@Test
	void hasAnnotation() {
		final var hasTestAnnotation = Stream.of(getClass())
			.flatMap(M::getMethods)
			.anyMatch(M.hasAnnotation(Test.class));

		assertTrue(hasTestAnnotation);

		final var doesNotHaveDisplayNameAnnotation = Stream.of(getClass())
			.flatMap(M::getMethods)
			.noneMatch(M.hasAnnotation(DisplayName.class));

		assertTrue(doesNotHaveDisplayNameAnnotation);
	}

	@Test
	void getAnnotations() {
		final var count = Stream.of(getClass()).flatMap(M::getAnnotations).count();

		assertEquals(1, count);
	}

	@Test
	void getTypeParameters() {
		// TODO
	}

	@Test
	void getBounds() {
		// TODO
	}

	@Test
	void getAnnotatedBounds() {
		// TODO
	}

	@Test
	void getParameterTypes() {
		final var parameterTypes = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(M.withEquals(Method::getName, "parameterFoo"))
			.flatMap(M::getParameterTypes)
			.toList();

		assertEquals(1, parameterTypes.size());
		assertEquals(Object.class, parameterTypes.get(0));
	}

	@Test
	void hasParameterCount() {
		final var parameterTypes = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(M.withEquals(Method::getName, "parameterFoo"))
			.filter(M.hasParameterCount(1))
			.flatMap(M::getParameterTypes)
			.toList();

		assertEquals(1, parameterTypes.size());
		assertEquals(Object.class, parameterTypes.get(0));
	}

	void parameterFoo(final Object o) {

	}

	@Test
	void getGenericParameterTypes() {
		// TODO
	}

	@Test
	void getParameters() {
		// TODO
	}

	@Test
	void getExceptionTypes() {
		// TODO
	}

	@Test
	void getGenericExceptionTypes() {
		// TODO
	}

	@Test
	void getParameterAnnotations() {
		// TODO
	}

	@Test
	void getAnnotatedParameterTypes() {
		// TODO
	}

	@Test
	void getAnnotatedExceptionTypes() {
		// TODO
	}

	@Test
	void getConstructors() {
		final var constructors = Stream.of(getClass()).flatMap(M::getConstructors).toList();

		assertEquals(1, constructors.size());
	}

	@Test
	void getFields() {
		final var fields = Stream.of(getClass()).flatMap(M::getFields).toList();

		assertEquals(7, fields.size());
	}

	@Test
	void get() {
		final var field = Stream.of(getClass())
			.flatMap(M::getFields)
			.filter(M.withEquals(Field::getName, "object"))
			.findFirst()
			.map(M.get(this));

		assertTrue(field.isPresent());
	}

	@Test
	void getStatic() {
		final var field = Stream.of(getClass())
			.flatMap(M::getFields)
			.filter(M.withEquals(Field::getName, "staticObject"))
			.findFirst()
			.map(M.getStatic());

		assertTrue(field.isPresent());
	}

	@Test
	void getMethods() {
		final var methods = Stream.of(getClass()).flatMap(M::getMethods).toList();

		assertTrue(methods.size() > 50);
	}

	@Test
	void invoke() {
		final var foo = new InvokeFoo();

		final var getNull = Stream.of(InvokeFoo.class)
			.flatMap(M::getMethods)
			.filter(M.withEquals(Method::getName, "getNull"))
			.map(M.invoke(foo))
			.filter(Objects::nonNull)
			.findFirst();
		assertTrue(getNull.isEmpty());

		final var getObject = Stream.of(InvokeFoo.class)
			.flatMap(M::getMethods)
			.filter(M.withEquals(Method::getName, "getObject"))
			.map(M.invoke(foo))
			.findFirst();
		assertTrue(getObject.isPresent());
	}

	@Test
	void invokeStatic() {
		final var getNull = Stream.of(InvokeFoo.class)
			.flatMap(M::getMethods)
			.filter(M.withEquals(Method::getName, "getStatic"))
			.map(M.invokeStatic())
			.filter(Objects::nonNull)
			.findFirst();
		assertTrue(getNull.isEmpty());
	}

	class InvokeFoo {

		void getNull() {

		}

		Object getObject() {
			return new Object();
		}

		static void getStatic() {

		}

	}

	@Test
	void getClasses() {
		final var count = Stream.of(getClass()).flatMap(M::getClasses).count();

		assertEquals(9, count);
	}

	@Test
	void isPublicClass() {
		final var thisIsPublic = Stream.of(getClass()).anyMatch(M::isPublic);

		assertFalse(thisIsPublic);

		final var isPublic = Stream.of(PublicFoo.class).anyMatch(M::isPublic);

		assertTrue(isPublic);
	}

	public class PublicFoo {

	}

	@Test
	void isPrivateClass() {
		final var thisIsPrivate = Stream.of(getClass()).anyMatch(M::isPrivate);

		assertFalse(thisIsPrivate);

		final var isPrivate = Stream.of(PrivateFoo.class).anyMatch(M::isPrivate);

		assertTrue(isPrivate);
	}

	private class PrivateFoo {

	}

	@Test
	void isProtectedClass() {
		final var thisIsProtected = Stream.of(getClass()).anyMatch(M::isProtected);

		assertFalse(thisIsProtected);

		final var isProtected = Stream.of(ProtectedFoo.class).anyMatch(M::isProtected);

		assertTrue(isProtected);
	}

	protected class ProtectedFoo {

	}

	@Test
	void isStaticClass() {
		final var thisIsStatic = Stream.of(getClass()).anyMatch(M::isStatic);

		assertFalse(thisIsStatic);

		final var isStatic = Stream.of(StaticFoo.class).anyMatch(M::isStatic);

		assertTrue(isStatic);
	}

	static class StaticFoo {

	}

	@Test
	void isFinalClass() {
		final var thisIsFinal = Stream.of(getClass()).anyMatch(M::isFinal);

		assertFalse(thisIsFinal);

		final var isFinal = Stream.of(FinalFoo.class).anyMatch(M::isFinal);

		assertTrue(isFinal);
	}

	final class FinalFoo {

	}

	@Test
	void isInterfaceClass() {
		final var thisIsInterface = Stream.of(getClass()).anyMatch(M::isInterface);

		assertFalse(thisIsInterface);

		final var isInterface = Stream.of(InterfaceFoo.class).anyMatch(M::isInterface);

		assertTrue(isInterface);
	}

	interface InterfaceFoo {

	}

	@Test
	void isAbstractClass() {
		final var thisIsAbstract = Stream.of(getClass()).anyMatch(M::isAbstract);

		assertFalse(thisIsAbstract);

		final var isAbstract = Stream.of(AbstractFoo.class).anyMatch(M::isAbstract);

		assertTrue(isAbstract);
	}

	abstract class AbstractFoo {

	}

	@Test
	void isPublic() {
		final var count = Stream.of(getClass()).flatMap(M::getFields).filter(M::isPublic).count();

		assertEquals(1, count);
	}

	@Test
	void isPrivate() {
		final var count = Stream.of(getClass()).flatMap(M::getFields).filter(M::isPrivate).count();

		assertEquals(1, count);
	}

	@Test
	protected void isProtected() {
		final var count = Stream.of(getClass())
			.flatMap(M::getFields)
			.filter(M::isProtected)
			.count();

		assertEquals(1, count);
	}

	@Test
	void isStatic() {
		final var count = Stream.of(getClass()).flatMap(M::getFields).filter(M::isStatic).count();

		assertEquals(1, count);
	}

	@Test
	final void isFinal() {
		final var count = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(M.hasAnnotation(Test.class))
			.filter(M::isFinal)
			.count();

		assertEquals(1, count);
	}

	@Test
	synchronized void isSynchronized() {
		final var count = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(M.hasAnnotation(Test.class))
			.filter(M::isSynchronized)
			.count();

		assertEquals(1, count);
	}

	@Test
	void isVolatile() {
		final var count = Stream.of(getClass()).flatMap(M::getFields).filter(M::isVolatile).count();

		assertEquals(1, count);
	}

	@Test
	void isTransient() {
		final var count = Stream.of(getClass())
			.flatMap(M::getFields)
			.filter(M::isTransient)
			.count();

		assertEquals(1, count);
	}

	@Test
	void isNative() {
		// TODO
	}

	@Test
	void isAbstractMember() {
		final var count = Stream.of(AbstractClassFoo.class)
			.flatMap(M::getMethods)
			.filter(M::isAbstract)
			.count();

		assertEquals(1, count);
	}

	abstract class AbstractClassFoo {

		abstract void doNothing();

	}

	@Test
	void with() {
		final var methods = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(M.with(Method::getName, "with"::equals))
			.toList();

		assertEquals(1, methods.size());
	}

	@Test
	void withArrayEquals() {
		final var methods = Stream.of(getClass())
			.flatMap(M::getMethods)
			.filter(M.withEquals(Method::getName, "withArrayEqualsFoo"))
			.filter(M.withArrayEquals(Method::getParameterTypes, String.class))
			.toList();

		assertEquals(1, methods.size());
	}

	void withArrayEqualsFoo(final String s) {
	}

}
