package com.tagadvance.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.spi.LocationAwareLogger;

/**
 * A {@link LocationAwareLogger} that records what it was asked to log. Built on slf4j-api alone,
 * so the tests do not depend on any backend.
 */
class RecordingLogger extends AbstractLogger implements LocationAwareLogger {

	record Event(Level level, Marker marker, String fqcn, String message, Object[] args,
				 Throwable throwable, String thread) {

	}

	final List<Event> events = Collections.synchronizedList(new ArrayList<>());

	RecordingLogger(final String name) {
		this.name = name;
	}

	@Override
	public void log(final Marker marker, final String fqcn, final int level, final String message,
		final Object[] argArray, final Throwable t) {
		events.add(new Event(levelOf(level), marker, fqcn, message, argArray, t,
			Thread.currentThread().getName()));
	}

	private static Level levelOf(final int level) {
		return switch (level) {
			case LocationAwareLogger.TRACE_INT -> Level.TRACE;
			case LocationAwareLogger.DEBUG_INT -> Level.DEBUG;
			case LocationAwareLogger.INFO_INT -> Level.INFO;
			case LocationAwareLogger.WARN_INT -> Level.WARN;
			default -> Level.ERROR;
		};
	}

	@Override
	protected void handleNormalizedLoggingCall(final Level level, final Marker marker,
		final String messagePattern, final Object[] arguments, final Throwable throwable) {
		events.add(new Event(level, marker, getFullyQualifiedCallerName(), messagePattern,
			arguments, throwable, Thread.currentThread().getName()));
	}

	@Override
	protected String getFullyQualifiedCallerName() {
		return RecordingLogger.class.getName();
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled(final Marker marker) {
		return true;
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isDebugEnabled(final Marker marker) {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled(final Marker marker) {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled(final Marker marker) {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled(final Marker marker) {
		return true;
	}

}
