# Example

Are you tired of writing code like this?

```java
void foo() throws Exception {
	try {
		Stream.empty().map(i -> {
			try {
				throw new Exception();
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}).forEach(System.err::println);
	} catch (final RuntimeException e) {
		final var cause = e.getCause();
		if (cause instanceof Exception) {
			throw (Exception) cause;
		}
	
		throw e;
	}
}
```

instead of code like this:

```java
void foo() throws Exception {
	Stream.empty()
		.map(CheckedFunction.of(this::throwsException))
		.forEach(System.err::println);
}

Object throwsException(final Object o) throws Exception {
	throw new Exception();
}
```

or this?

```java
void foo() throws Exception {
	final var logger = LoggerFactory.getLogger(getClass());
	final var deferredException = new DeferredException(e -> logger.error(e.getMessage()));
	Stream.empty()
		.map(deferredException.function(this::throwsException))
		.filter(Objects::nonNull)
		.forEach(System.err::println);
}

Object throwsException(final Object o) throws Exception {
	throw new Exception();
}
```
