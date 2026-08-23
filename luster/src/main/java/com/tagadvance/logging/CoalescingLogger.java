package com.tagadvance.logging;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Wraps a real {@link Logger}, passing the first occurrence of a message straight through and
 * handing the repeats to the {@link CoalescingLoggerFactory factory} to be counted.
 * <p>
 * Extending {@link AbstractLogger} funnels all fifty-odd {@link Logger} methods into one hook, so
 * there is no proxy and no reflection.
 */
final class CoalescingLogger extends AbstractLogger {

	/**
	 * slf4j's {@link AbstractLogger} is {@link java.io.Serializable}; this logger is not
	 * meaningfully serializable, but the identifier keeps the compiler quiet without pretending
	 * otherwise.
	 */
	private static final long serialVersionUID = 1L;

	private static final String FQCN = CoalescingLogger.class.getName();

	private final Logger delegate;

	private final CoalescingLoggerFactory factory;

	CoalescingLogger(final String name, final Logger delegate,
		final CoalescingLoggerFactory factory) {
		this.name = requireNonNull(name, "name must not be null");
		this.delegate = requireNonNull(delegate, "delegate must not be null");
		this.factory = requireNonNull(factory, "factory must not be null");
	}

	@Override
	protected String getFullyQualifiedCallerName() {
		return FQCN;
	}

	@Override
	protected void handleNormalizedLoggingCall(final Level level, final @Nullable Marker marker,
		final String messagePattern, final Object @Nullable [] arguments,
		final @Nullable Throwable throwable) {
		factory.log(this, level, marker, messagePattern, arguments, throwable);
	}

	/**
	 * Logs an event through to the real logger unchanged.
	 * <p>
	 * Passing our own name as the fully-qualified caller name lets a
	 * {@link LocationAwareLogger} skip our frames when it computes {@code %class}, {@code %method}
	 * and {@code %line}, so caller data points at the code that actually logged. This is
	 * {@code org.slf4j.spi}, not a backend-specific hook.
	 */
	void emit(final Level level, final @Nullable Marker marker, final String messagePattern,
		final Object @Nullable [] arguments, final @Nullable Throwable throwable) {
		if (delegate instanceof final LocationAwareLogger aware) {
			aware.log(marker, FQCN, level.toInt(), messagePattern, arguments, throwable);

			return;
		}

		// slf4j extracts a trailing Throwable from the argument array
		final var args = append(arguments, throwable);
		switch (level) {
			case ERROR -> {
				if (marker == null) {
					delegate.error(messagePattern, args);
				} else {
					delegate.error(marker, messagePattern, args);
				}
			}
			case WARN -> {
				if (marker == null) {
					delegate.warn(messagePattern, args);
				} else {
					delegate.warn(marker, messagePattern, args);
				}
			}
			case INFO -> {
				if (marker == null) {
					delegate.info(messagePattern, args);
				} else {
					delegate.info(marker, messagePattern, args);
				}
			}
			case DEBUG -> {
				if (marker == null) {
					delegate.debug(messagePattern, args);
				} else {
					delegate.debug(marker, messagePattern, args);
				}
			}
			case TRACE -> {
				if (marker == null) {
					delegate.trace(messagePattern, args);
				} else {
					delegate.trace(marker, messagePattern, args);
				}
			}
		}
	}

	private static Object[] append(final Object @Nullable [] arguments,
		final @Nullable Throwable throwable) {
		final var args = arguments == null ? new Object[0] : arguments;
		if (throwable == null) {
			return args;
		}

		final var appended = Arrays.copyOf(args, args.length + 1);
		appended[args.length] = throwable;

		return appended;
	}

	@Override
	public boolean isTraceEnabled() {
		return delegate.isTraceEnabled();
	}

	@Override
	public boolean isTraceEnabled(final Marker marker) {
		return delegate.isTraceEnabled(marker);
	}

	@Override
	public boolean isDebugEnabled() {
		return delegate.isDebugEnabled();
	}

	@Override
	public boolean isDebugEnabled(final Marker marker) {
		return delegate.isDebugEnabled(marker);
	}

	@Override
	public boolean isInfoEnabled() {
		return delegate.isInfoEnabled();
	}

	@Override
	public boolean isInfoEnabled(final Marker marker) {
		return delegate.isInfoEnabled(marker);
	}

	@Override
	public boolean isWarnEnabled() {
		return delegate.isWarnEnabled();
	}

	@Override
	public boolean isWarnEnabled(final Marker marker) {
		return delegate.isWarnEnabled(marker);
	}

	@Override
	public boolean isErrorEnabled() {
		return delegate.isErrorEnabled();
	}

	@Override
	public boolean isErrorEnabled(final Marker marker) {
		return delegate.isErrorEnabled(marker);
	}

}
