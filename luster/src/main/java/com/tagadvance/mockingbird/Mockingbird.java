package com.tagadvance.mockingbird;

import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import com.tagadvance.proxy.Invocation;
import com.tagadvance.proxy.InvocationProxy;
import com.tagadvance.reflection.M;
import com.tagadvance.stack.Annotations;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mockingbird {

	private static final Logger logger = LoggerFactory.getLogger(Mockingbird.class);

	private final Annotations annotations;
	private final InvocationNameGenerator generator;
	private final Gson gson;

	public Mockingbird(final Annotations annotations) {
		this(annotations, new CountingInvocationNameGenerator());
	}

	public Mockingbird(final Annotations annotations, final InvocationNameGenerator generator) {
		this(annotations, generator, new Gson());
	}

	public Mockingbird(final Annotations annotations, final InvocationNameGenerator generator,
		final Gson gson) {
		this.annotations = requireNonNull(annotations, "annotations must not be null");
		this.generator = requireNonNull(generator, "generator must not be null");
		this.gson = requireNonNull(gson, "gson must not be null");
	}

	/**
	 * @param iface    an interface
	 * @param instance an instance of {@literal iface}
	 * @param <I>      the interface type
	 * @return a proxy
	 */
	public <I> I createProxy(final Class<I> iface, final I instance) {
		return (I) InvocationProxy.createProxy(iface, instance, this::onInvocation);
	}

	private Object onInvocation(final Invocation invocation) throws Throwable {
		if (logger.isTraceEnabled()) {
			logger.trace("onInvocation {}#{}({})", invocation.instance().getClass().getSimpleName(),
				invocation.method().getName(), Stream.of(invocation.args())
					.map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
					.collect(Collectors.joining(", ")));
		}

		final var mimic = mimic();
		if (mimic.isEmpty() || isObjectMethod(invocation)) {
			logger.debug("deferring to invocation");

			return invocation.invoke();
		}

		final var path = mimic.get();
		final var mimicName = generator.toName(invocation);
		final var mimicPath = path.resolve(mimicName);
		if (Files.isReadable(mimicPath)) {
			logger.debug("Reading mimic from {}", mimicPath);
			try (final var reader = Files.newBufferedReader(mimicPath)) {
				final var className = reader.readLine();
				final var aClass = Class.forName(className);
				final var result = gson.fromJson(reader, aClass);

				return result;
			}
		}

		final var result = invocation.invoke();
		logger.debug("Writing mimic to {}", mimicPath);
		try (final var writer = Files.newBufferedWriter(mimicPath)) {
			writer.write(result.getClass().getName());
			writer.write(System.lineSeparator());

			final var json = gson.toJson(result);
			writer.append(json);
		}

		return result;
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
	 * @return the mimic configuration, if present
	 */
	private Optional<Path> mimic() {
		return annotations.getAnnotations()
			.filter(M.withEquals(Annotation::annotationType, Mimic.class))
			.findFirst()
			.map(a -> ((Mimic) a).path())
			.map(Path::of);
	}

}
