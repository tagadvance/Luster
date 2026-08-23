# Mirror

Mirror is a utility that makes reflection easy!

## Examples

```java
// get the value of a static field named "foo"
final var fooValue = Stream.of(getClass())
	.flatMap(M::getFields)
	.filter(M.withEquals(Field::getName, "foo"))
	.filter(M.canAccessStatic())
	.map(M.getStatic())
	.findFirst().get();
```

`canAccessStatic()` rejects instance members rather than testing them, so it is safe to apply to a
mixed stream; no `filter(M::isStatic)` is needed in front of it.

```java
// get all methods annotated with @Test
final var testMethods = Stream.of(getClass())
	.flatMap(M::getMethods)
	.filter(M.hasAnnotation(Test.class))
	.toList();
```

```java
// get all methods with a single parameter of type Object
final var testMethods = Stream.of(getClass())
	.flatMap(M::getMethods)
	.filter(M.withArrayEquals(Method::getParameterTypes, Object.class))
	.toList();
```

```java
// get classes with name that starts with "Foo"
final var fooClasses = Stream.of(getClass())
	.flatMap(M::getClasses)
	.filter(M.with(Class::getSimpleName, s -> s.startsWith("Foo")))
	.toList();
```

```java
// get every field in the hierarchy, including the non-public ones an inherited
// getFields() cannot see
final var allFields = Stream.of(getClass())
	.flatMap(M::getAllFields)
	.toList();
```

```java
// walk the hierarchy: getSuperclasses() replaces the usual while (c != null) loop
final var declaredMethodCount = M.getSuperclasses(getClass())
	.flatMap(c -> Stream.of(c.getDeclaredMethods()))
	.count();
```

```java
// a generic override yields both the real method and a compiler-generated bridge;
// drop the bridge
final var compareTo = Stream.of(getClass())
	.flatMap(M::getMethods)
	.filter(M.withEquals(Method::getName, "compareTo"))
	.filter(Predicate.not(M::isBridge))
	.findFirst();
```

## Caching

`Class#getDeclaredMethods()` and its siblings clone their backing array on every call, so a stream
built over a hot class allocates on every pass. `Mirror.cached()` returns a view whose lookups are
memoized per class, backed by a `ClassValue`:

```java
final var methods = M.cached().getMethods(getClass()).toList();
```

Caching is an explicit entry point rather than a silent optimization because the cache is never
invalidated: a class redefined at runtime, by an instrumentation agent or a hot-reloading
container, keeps serving the members it had when it was first seen. Cached members are replayed
rather than copied, so `setAccessible` on one of them is visible to every other caller of the view.

## Merge semantics

`getFields`, `getMethods`, `getConstructors` and `getClasses` merge `getDeclaredX()` with
`getX()`. That is *declared members of this class* plus *public members of the whole hierarchy* --
neither "declared" nor "all":

* a non-public member inherited from a superclass is invisible; use `getAllFields` or
  `getAllMethods` to walk the hierarchy;
* bridge and synthetic methods come through; filter them with `M::isBridge` or `M::isSynthetic`;
* `distinct()` leans on `Method#equals`, which includes the declaring class, so an overridden
  method appears once per class in the hierarchy that declares it.
