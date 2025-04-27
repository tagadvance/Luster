package com.tagadvance.logging;

import static java.util.Objects.requireNonNull;

import com.tagadvance.proxy.Invocation;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * A {@link LogEntry log entry}.
 */
public final class LogEntry {

	private final Instant instant;
	private final Level level;
	private final Marker marker;
	private final String format;
	private final Object[] args;
	private final Throwable throwable;

	static Optional<LogEntry> fromInvocation(final Invocation invocation) {
		final var levelsByName = getLevelsByName();
		final var methodName = invocation.method().getName().toUpperCase();
		final var level = levelsByName.get(methodName);
		if (level == null) {
			return Optional.empty();
		}

		final var parameterTypes = invocation.method().getParameterTypes();
		final var args = invocation.args();

		Marker marker = null;
		String format = null;
		Object[] arguments = null;
		int argumentCount = 0;
		Throwable throwable = null;
		for (int i = 0; i < parameterTypes.length; i++) {
			final var parameterType = parameterTypes[i];
			if (Marker.class.equals(parameterType)) {
				marker = (Marker) args[i];
			} else if (String.class.equals(parameterType)) {
				format = (String) args[i];
			} else if (Object[].class.equals(parameterType)) {
				arguments = (Object[]) args[i];
			} else if (Object.class.equals(parameterType)) {
				arguments = new Object[2];
				arguments[argumentCount++] = args[i];
			} else if (Throwable.class.equals(parameterType)) {
				throwable = (Throwable) args[i];
			}
		}

		final var logEntry = new LogEntry(level, marker, format, arguments, throwable);

		return Optional.of(logEntry);
	}

	LogEntry(final Level level, final String message) {
		this(Instant.now(), level, null, message, null, null);
	}

	LogEntry(final Level level, final String message, final Throwable throwable) {
		this(Instant.now(), level, null, message, null, throwable);
	}

	LogEntry(final Level level, final String format, final Object... args) {
		this(Instant.now(), level, null, format, args, null);
	}

	private LogEntry(final Level level, final Marker marker, final String format,
		final Object[] args, final Throwable throwable) {
		this(Instant.now(), level, marker, format, args, throwable);
	}

	private LogEntry(final Instant instant, final Level level, final Marker marker, String format,
		final Object[] args, final Throwable throwable) {
		this.instant = requireNonNull(instant, "instant must not be null");
		this.level = requireNonNull(level, "level must not be null");
		this.marker = marker;
		this.format = requireNonNull(format, "format must not be null");
		this.args = args;
		this.throwable = throwable;
	}

	public Instant getInstant() {
		return instant;
	}

	public Level getLevel() {
		return level;
	}

	public Optional<Marker> getMarker() {
		return Optional.ofNullable(marker);
	}

	public String getFormat() {
		return format;
	}

	public Object[] getArgs() {
		return args;
	}

	public Optional<Throwable> getThrowable() {
		return Optional.ofNullable(throwable);
	}

	public void log(final Logger logger) {
		Class<? extends Logger> lClass = logger.getClass();
		final var level = this.level.name().toLowerCase();
		final var parameterArray = toParameterArray();
		final var argumentArray = toArgumentArray();
		try {
			lClass.getDeclaredMethod(level, parameterArray).invoke(logger, argumentArray);
		} catch (final NoSuchMethodException | IllegalAccessException |
					   InvocationTargetException e) {
			logger.error("", e);
		}
	}

	private Class<?>[] toParameterArray() {
		final var list = new ArrayList<Class<?>>();

		if (marker != null) {
			list.add(Marker.class);
		}
		if (format != null) {
			list.add(String.class);
		}
		if (args != null) {
			if (args.length > 0) {
				list.add(Object.class);
			}
			if (args.length > 1) {
				list.add(Object.class);
			}
		}
		if (throwable != null) {
			list.add(Throwable.class);
		}

		return list.toArray(new Class<?>[]{});
	}

	private Object[] toArgumentArray() {
		final var list = new ArrayList<>();
		getMarker().ifPresent(list::add);
		if (format != null) {
			list.add(format);
		}
		if (args != null) {
			list.addAll(Arrays.asList(args));
		}
		getThrowable().ifPresent(list::add);

		return list.toArray();
	}

	// TODO: unit test
	@Override
	public String toString() {
		final String templateField = "{}";
		final StringBuilder builder = new StringBuilder(format);

		if (args != null) {
			for (final Object arg : args) {
				final var i = builder.indexOf(templateField);
				if (i < 0) {
					break;
				}

				final var argString = Optional.of(arg).map(Object::toString).orElse("null");
				builder.replace(i, i + templateField.length(), argString);
			}
		}

		return builder.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof final LogEntry logEntry)) {
			return false;
		}

		return Objects.equals(instant, logEntry.instant) && level == logEntry.level
			&& Objects.equals(marker, logEntry.marker) && Objects.equals(format, logEntry.format)
			&& Objects.deepEquals(args, logEntry.args) && Objects.equals(throwable,
			logEntry.throwable);
	}

	@Override
	public int hashCode() {
		return Objects.hash(instant, level, marker, format, Arrays.hashCode(args), throwable);
	}

	private static Map<String, Level> getLevelsByName() {
		return Stream.of(Level.values())
			.collect(Collectors.toMap(Enum::name, Function.identity(), (a, b) -> {
				throw new UnsupportedOperationException();
			}, LinkedHashMap::new));
	}

}
