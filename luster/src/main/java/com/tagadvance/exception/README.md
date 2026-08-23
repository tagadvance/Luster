# `com.tagadvance.exception`

Two families of functional interface, for two different jobs.

## Adapters — get a throwing lambda past the JDK

`CheckedFunction`, `CheckedConsumer`, `CheckedPredicate`, `CheckedRunnable`,
`CheckedComparator` extend the JDK type so they can be handed to it. Are you tired of
writing code like this?

```java
void foo() throws IOException {
	try {
		Stream.of(paths).map(path -> {
			try {
				return Files.readString(path);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}).forEach(System.err::println);
	} catch (final RuntimeException e) {
		final var cause = e.getCause();
		if (cause instanceof IOException io) {
			throw io;
		}

		throw e;
	}
}
```

instead of code like this?

```java
void foo() throws IOException {
	Checked.rethrowing(IOException.class, () -> Stream.of(paths)
		.map(CheckedFunction.of(Files::readString))
		.forEach(System.err::println));
}
```

The adapters rethrow checked exceptions as `UncheckedException`, because the JDK signature
forbids anything else. `Checked.rethrowing` is the other half: it puts the original
exception back on the wire at exactly one place, where the compiler can enforce the
`throws` clause again. Without it the boilerplate has only moved to your caller.

Unchecked exceptions are **not** wrapped — an `IllegalArgumentException` thrown inside the
lambda comes out as itself, so existing `catch` blocks keep firing.

One exception type per `rethrowing` call; nest them if you need two. `throws E` cannot be
expressed over a varargs of type tokens.

## Throwing types — declare an honest `throws` in your own API

`ThrowingRunnable`, `ThrowingConsumer`, `ThrowingFunction`, `ThrowingPredicate`,
`ThrowingComparator`, `ThrowingSupplier` extend nothing and propagate
`E` to the caller. Use them as parameter types where you want `E` visible:

```java
default <V, E extends Exception> V readLock(ThrowingSupplier<V, E> supplier) throws E {
	...
}
```

Each adapter extends its throwing counterpart, so anything accepting a throwing type also
accepts the matching adapter. There is no `ThrowingCallable`: `Callable.call()` declares
`throws Exception`, which erases `E`, so `ThrowingSupplier` covers that role too.

## `OnError` — handle each failure and keep going

```java
final var onError = OnError.of(logger::error);
Stream.of(paths)
	.map(onError.optionalFunction(Files::readString))
	.flatMap(Optional::stream)
	.forEach(System.err::println);
```

`optionalFunction` and `optionalSupplier` are preferred over the `null`-defaulting
variants, which cannot distinguish "failed" from "legitimately returned null".

Unlike the adapters, `OnError` catches unchecked exceptions too — handle-and-continue is
the whole point of the type.

## `ExceptionCollector` — collect everything, throw once

```java
try (final var collector = ExceptionCollector.create()) {
	tenants.forEach(collector.consumer(this::sync));
} // throws if any tenant failed, first as the cause, rest suppressed
```

`close()` throws `UncheckedException` so that try-with-resources does not force every
caller to catch `Exception`; wrap the block in `Checked.rethrowing` to get the original
type back.
