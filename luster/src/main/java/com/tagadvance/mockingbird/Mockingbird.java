package com.tagadvance.mockingbird;

import static java.util.Objects.requireNonNull;

import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationProxy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records what a collaborator actually returned, then replays it.
 * <p>
 * Unlike VCR-style tools, which record at the HTTP layer, Mockingbird records at the interface
 * boundary — so it works for JDBC wrappers, vendor SDKs and internal services alike. It is also a
 * way to capture an interaction in an environment you cannot attach a debugger to, and replay it
 * locally.
 * <p>
 * Recordings are grouped in a directory per interface, one file per invocation:
 * <pre>
 * &lt;path&gt;/com.example.TenantApi/3f2504e04f8911d3.0000.gson
 * </pre>
 * <p>
 * <strong>Recorded values have been through a serialization round trip.</strong> Transient fields,
 * object identity, cycles and custom {@code equals} behaviour can all come back subtly different.
 * This is a recorded <em>mock</em>, not a recorded object.
 * <p>
 * Not safe for concurrent use: invocations of the same signature are numbered in the order they
 * arrive, so concurrent calls record and replay in a nondeterministic order.
 */
public class Mockingbird {

	private static final Logger logger = LoggerFactory.getLogger(Mockingbird.class);

	private static final String RETURNED = "RETURNED";

	private static final String THREW = "THREW";

	private final Path path;

	private final Mode mode;

	private final InvocationNameGenerator generator;

	private final MimicSerializer serializer;

	/**
	 * @param path where recordings live
	 */
	public Mockingbird(final Path path) {
		this(path, Mode.AUTO);
	}

	/**
	 * @param path where recordings live
	 * @param mode whether to record, replay, or decide per invocation
	 */
	public Mockingbird(final Path path, final Mode mode) {
		this(path, mode, null, null);
	}

	/**
	 * @param path       where recordings live
	 * @param mode       whether to record, replay, or decide per invocation
	 * @param generator  names the recordings, or {@literal null} for the default
	 * @param serializer reads and writes them, or {@literal null} for JSON
	 */
	public Mockingbird(final Path path, final Mode mode,
		final @Nullable InvocationNameGenerator generator,
		final @Nullable MimicSerializer serializer) {
		this.path = requireNonNull(path, "path must not be null");
		this.mode = requireNonNull(mode, "mode must not be null");
		this.serializer = Optional.ofNullable(serializer).orElseGet(JsonMimicSerializer::new);
		this.generator = Optional.ofNullable(generator)
			.orElseGet(() -> new CountingInvocationNameGenerator(
				CountingInvocationNameGenerator.MatchType.ARGUMENTS, this.serializer));
	}

	/**
	 * @param iface    an interface
	 * @param instance an instance of {@literal iface}
	 * @param <I>      the interface type
	 * @return a proxy
	 */
	public <I> I createProxy(final Class<I> iface, final I instance) {
		requireNonNull(iface, "iface must not be null");
		requireNonNull(instance, "instance must not be null");

		return InvocationProxy.createProxy(iface, instance,
			invocation -> onInvocation(iface, invocation));
	}

	private <I> Object onInvocation(final Class<I> iface, final Invocation invocation)
		throws Throwable {
		if (logger.isTraceEnabled()) {
			logger.trace("onInvocation {}#{}({})", invocation.instance().getClass().getSimpleName(),
				invocation.method().getName(), Stream.of(invocation.args())
					.map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
					.collect(Collectors.joining(", ")));
		}

		if (isObjectMethod(invocation)) {
			logger.debug("Deferring to invocation");

			return invocation.invoke();
		}

		final var mimicPath = path.resolve(iface.getName())
			.resolve(generator.toName(iface, invocation));

		if (mode != Mode.RECORD && Files.isReadable(mimicPath)) {
			logger.debug("Reading mimic from {}", mimicPath);

			return replay(mimicPath, invocation.method());
		}

		if (mode == Mode.REPLAY) {
			throw new MimicReplayException(
				"no recording at %s; run with %s to create one".formatted(mimicPath, Mode.AUTO));
		}

		return record(mimicPath, invocation);
	}

	private Object replay(final Path mimicPath, final Method method) throws Throwable {
		try (final var reader = Files.newBufferedReader(mimicPath)) {
			final var outcome = reader.readLine();
			if (THREW.equals(outcome)) {
				throw toThrowable(mimicPath, reader);
			}

			if (!RETURNED.equals(outcome)) {
				throw new MimicReplayException(
					"%s does not start with %s or %s".formatted(mimicPath, RETURNED, THREW));
			}

			// a void method has nothing to deserialize, and no serializer can represent its type
			return isVoid(method) ? null : serializer.read(reader, method.getGenericReturnType());
		}
	}

	/**
	 * Rebuilds a recorded failure. Only its type and message survive a recording, so the stack
	 * trace belongs to the replay rather than the original call.
	 */
	private Throwable toThrowable(final Path mimicPath, final BufferedReader reader)
		throws IOException {
		final var typeName = reader.readLine();
		final var message = reader.lines().collect(Collectors.joining(System.lineSeparator()));
		final Class<?> type;
		try {
			type = Class.forName(typeName);
		} catch (final ClassNotFoundException e) {
			return new MimicReplayException(
				"%s recorded a %s, which is not on the classpath: %s".formatted(mimicPath, typeName,
					message), e);
		}

		return construct(type, message).orElseGet(() -> new MimicReplayException(
			"%s recorded a %s, which has no (String) or no-argument constructor: %s".formatted(
				mimicPath, typeName, message)));
	}

	private static Optional<Throwable> construct(final Class<?> type, final String message) {
		try {
			return Optional.of((Throwable) type.getConstructor(String.class).newInstance(message));
		} catch (final NoSuchMethodException | InstantiationException | IllegalAccessException |
					   InvocationTargetException | ClassCastException ignored) {
			// fall through to the no-argument constructor
		}

		try {
			return Optional.of((Throwable) type.getConstructor().newInstance());
		} catch (final NoSuchMethodException | InstantiationException | IllegalAccessException |
					   InvocationTargetException | ClassCastException ignored) {
			return Optional.empty();
		}
	}

	private Object record(final Path mimicPath, final Invocation invocation) throws Throwable {
		final Object result;
		try {
			result = invocation.invoke();
		} catch (final Throwable throwable) {
			logger.debug("Writing failed mimic to {}", mimicPath);
			write(mimicPath, writer -> {
				writer.write(THREW);
				writer.write(System.lineSeparator());
				writer.write(throwable.getClass().getName());
				writer.write(System.lineSeparator());
				writer.write(Optional.ofNullable(throwable.getMessage()).orElse(""));
			});

			throw throwable;
		}

		final var method = invocation.method();
		logger.debug("Writing mimic to {}", mimicPath);
		write(mimicPath, writer -> {
			writer.write(RETURNED);
			writer.write(System.lineSeparator());
			if (!isVoid(method)) {
				serializer.write(writer, result, method.getGenericReturnType());
			}
		});

		return result;
	}

	/**
	 * Writes beside the target and moves into place, so an interrupted run cannot leave a
	 * half-written recording that later reads as valid.
	 */
	private static void write(final Path mimicPath, final Body body) throws IOException {
		final var directory = mimicPath.getParent();
		Files.createDirectories(directory);

		final var temporary = Files.createTempFile(directory, "mimic", ".tmp");
		try {
			try (final var writer = Files.newBufferedWriter(temporary)) {
				body.writeTo(writer);
			}

			Files.move(temporary, mimicPath, StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE);
		} catch (final IOException e) {
			Files.deleteIfExists(temporary);

			throw e;
		}
	}

	@FunctionalInterface
	private interface Body {

		void writeTo(Writer writer) throws IOException;

	}

	private static boolean isVoid(final Method method) {
		final var type = method.getReturnType();

		return type == void.class || type == Void.class;
	}

	/**
	 * The debugger calls toString repeatedly. These calls should not be mimicked.
	 *
	 * @param invocation an {@link Invocation invocation}
	 * @return {@literal true} if the {@link Method#getDeclaringClass() declaring class} is
	 * {@link Object}
	 */
	private boolean isObjectMethod(final Invocation invocation) {
		final var method = invocation.method();

		return method.getDeclaringClass() == Object.class;
	}

	/**
	 * Whether an invocation is recorded or replayed.
	 */
	public enum Mode {

		/**
		 * Always call through and overwrite the recording.
		 */
		RECORD,

		/**
		 * Always replay, and fail loudly if there is no recording. This is what you want in CI:
		 * a missing recording is a broken test, not a licence to call the real service.
		 */
		REPLAY,

		/**
		 * Replay when a recording exists, record when it does not.
		 */
		AUTO

	}

}
