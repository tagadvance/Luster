package com.tagadvance.reflection;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cached Mirror Test")
class CachedMirrorTest {

	@Test
	@DisplayName("cached() returns a shared view")
	void shared() {
		assertSame(Mirror.cached(), M.cached());
	}

	@Test
	@DisplayName("the cached view returns the same content as the uncached lookups")
	void sameContent() {
		final var cached = Mirror.cached();
		final var c = ArrayList.class;

		assertEquals(M.getConstructors(c).toList(), cached.getConstructors(c).toList());
		assertEquals(M.getFields(c).toList(), cached.getFields(c).toList());
		assertEquals(M.getMethods(c).toList(), cached.getMethods(c).toList());
		assertEquals(M.getClasses(c).toList(), cached.getClasses(c).toList());
		assertEquals(M.getSuperclasses(c).toList(), cached.getSuperclasses(c).toList());
		assertEquals(M.getInterfaces(c).toList(), cached.getInterfaces(c).toList());
		assertEquals(M.getAllFields(c).toList(), cached.getAllFields(c).toList());
		assertEquals(M.getAllMethods(c).toList(), cached.getAllMethods(c).toList());
		assertEquals(M.getRecordComponents(Pair.class).map(RecordComponent::getName).toList(),
			cached.getRecordComponents(Pair.class).map(RecordComponent::getName).toList());
	}

	@Test
	@DisplayName("the cached view replays the very same members")
	void memoized() {
		final var cached = Mirror.cached();
		final var first = cached.getMethods(String.class).toList();
		final var second = cached.getMethods(String.class).toList();

		assertEquals(first.size(), second.size());
		for (var i = 0; i < first.size(); i++) {
			assertSame(first.get(i), second.get(i));
		}
	}

	record Pair(String left, String right) {

	}

}
