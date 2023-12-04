# Mirror

Mirror is a utility that makes reflection easy!

## Examples

```java
// get the value of a static field named "foo"
final var fooValue = Stream.of(getClass())
	.flatMap(M::getFields)
	.filter(M::isStatic)
	.filter(M.withEquals(Field::getName, "foo"))
	.filter(M.canAccessStatic())
	.map(M.getStatic())
	.findFirst().get();
```

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
