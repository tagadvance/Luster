# Luster

[![Build](https://github.com/tagadvance/Luster/actions/workflows/build.yml/badge.svg)](https://github.com/tagadvance/Luster/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tagadvance/luster)](https://central.sonatype.com/artifact/com.tagadvance/luster)
[![Release](https://img.shields.io/github/v/release/tagadvance/Luster)](https://github.com/tagadvance/Luster/releases)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Small, sharp tools for Java. Each package solves one problem and gets out of the way.

Requires **Java 17**. Depends on slf4j-api and JSpecify; Guava and Gson are internal.

## Installation

```kotlin
implementation("com.tagadvance:luster:3.0.0")
```

```xml
<dependency>
  <groupId>com.tagadvance</groupId>
  <artifactId>luster</artifactId>
  <version>3.0.0</version>
</dependency>
```

## What is in it

### `cache` — declarative caching by proxy

Declare caching on an interface; the implementation stays ignorant of it.

```java
public interface TenantApi {

	@CacheConfiguration(name = "tenants", expireAfterWrite = "PT5M")
	List<Tenant> getTenants();

}

final TenantApi api = Caches.newCache(TenantApi.class, new HttpTenantApi());
```

Interfaces owned by a dependency can be annotated with a **mask** — a sub-interface that
redeclares the methods you want cached. See the `cache` package documentation.

### `logging` — log once, then count

A burst of identical errors becomes one full log line plus one summary, instead of forty
thousand stack traces. The first occurrence goes through synchronously, so its timestamp, MDC,
thread and caller data are all real.

```java
try (final var factory = CoalescingLoggerFactory.builder()
		.withQuietPeriod(Duration.ofSeconds(6))
		.build()) {
	final Logger logger = factory.getLogger(MyJob.class.getName());
}
```

### `locks` — read/write locks with scope

```java
final ScopedLock lock = Locks.newLock();

try (final var ignored = lock.write()) {
	map.put(key, value);
}

final var tenants = lock.readLock(this::loadTenants);
```

`DeadlockDetector` finds cycles through locks held in *shared* mode — the case
`ThreadMXBean.findDeadlockedThreads()` is structurally blind to, because a read lock records no
owner for the JVM to follow.

### `mockingbird` — record and replay a collaborator

Records at the interface boundary rather than over HTTP, so it works for JDBC wrappers, vendor
SDKs and internal services alike.

```java
final var api = new Mockingbird(path, Mockingbird.Mode.REPLAY).createProxy(TenantApi.class, real);
```

### `reflection` — reflection as streams

```java
M.getAllFields(Tenant.class)
	.filter(M::isStatic)
	.map(M.getStatic())
	.forEach(System.out::println);
```

### `exception` — checked exceptions in lambdas, honestly

Adapters get a throwing lambda past a JDK signature; `Checked.rethrowing` puts the original
exception back at the boundary, so the `throws` clause means something again.

```java
void load() throws IOException {
	Checked.rethrowing(IOException.class, () -> Stream.of(paths)
		.map(CheckedFunction.of(Files::readString))
		.forEach(System.err::println));
}
```

### Also

* `debounce` — trailing and leading debouncers with `maxWait` and `flush`
* `shell` — run a subprocess, read both streams, await the exit code
* `shutdown` — report threads that outlive a shutdown request, with pruned stack traces
* `staging` — split work into a cancellable phase and a short atomic commit
* `stack` — `StackWalker`-based stack inspection and pruning
* `proxy` — the three-interface proxy layer the above is built on
* `utilities` — `Once`, `Sleep`, `Timed`, `Patterns`, `OperatingSystem`

## Package documentation

* [exception](luster/src/main/java/com/tagadvance/exception/README.md)
* [reflection](luster/src/main/java/com/tagadvance/reflection/README.md)

## Building

```sh
./gradlew build
```

The build is warning-free and is kept that way: `-Xlint:all` and full javadoc doclint are both
enabled, and every public element is documented.

## License

[Apache License 2.0](LICENSE). Releases up to and including 2.1.0 were published under the MIT
license.
