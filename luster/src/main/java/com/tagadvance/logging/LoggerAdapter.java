package com.tagadvance.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;

public interface LoggerAdapter extends Logger {

	@Override
	default String getName() {
		return "";
	}

	@Override
	default boolean isTraceEnabled() {
		return true;
	}

	@Override
	default void trace(final String msg) {

	}

	@Override
	default void trace(final String format, final Object arg) {

	}

	@Override
	default void trace(final String format, final Object arg1, final Object arg2) {

	}

	@Override
	default void trace(final String format, final Object... arguments) {

	}

	@Override
	default void trace(final String msg, final Throwable t) {

	}

	@Override
	default boolean isTraceEnabled(final Marker marker) {
		return true;
	}

	@Override
	default void trace(final Marker marker, final String msg) {

	}

	@Override
	default void trace(final Marker marker, final String format, final Object arg) {

	}

	@Override
	default void trace(final Marker marker, final String format, final Object arg1,
		final Object arg2) {

	}

	@Override
	default void trace(final Marker marker, final String format, final Object... argArray) {

	}

	@Override
	default void trace(final Marker marker, final String msg, final Throwable t) {

	}

	@Override
	default boolean isDebugEnabled() {
		return true;
	}

	@Override
	default void debug(final String msg) {

	}

	@Override
	default void debug(final String format, final Object arg) {

	}

	@Override
	default void debug(final String format, final Object arg1, final Object arg2) {

	}

	@Override
	default void debug(final String format, final Object... arguments) {

	}

	@Override
	default void debug(final String msg, final Throwable t) {

	}

	@Override
	default boolean isDebugEnabled(final Marker marker) {
		return true;
	}

	@Override
	default void debug(final Marker marker, final String msg) {

	}

	@Override
	default void debug(final Marker marker, final String format, final Object arg) {

	}

	@Override
	default void debug(final Marker marker, final String format, final Object arg1,
		final Object arg2) {

	}

	@Override
	default void debug(final Marker marker, final String format, final Object... arguments) {

	}

	@Override
	default void debug(final Marker marker, final String msg, final Throwable t) {

	}

	@Override
	default boolean isInfoEnabled() {
		return true;
	}

	@Override
	default void info(final String msg) {

	}

	@Override
	default void info(final String format, final Object arg) {

	}

	@Override
	default void info(final String format, final Object arg1, final Object arg2) {

	}

	@Override
	default void info(final String format, final Object... arguments) {

	}

	@Override
	default void info(final String msg, final Throwable t) {

	}

	@Override
	default boolean isInfoEnabled(final Marker marker) {
		return true;
	}

	@Override
	default void info(final Marker marker, final String msg) {

	}

	@Override
	default void info(final Marker marker, final String format, final Object arg) {

	}

	@Override
	default void info(final Marker marker, final String format, final Object arg1,
		final Object arg2) {

	}

	@Override
	default void info(final Marker marker, final String format, final Object... arguments) {

	}

	@Override
	default void info(final Marker marker, final String msg, final Throwable t) {

	}

	@Override
	default boolean isWarnEnabled() {
		return true;
	}

	@Override
	default void warn(final String msg) {

	}

	@Override
	default void warn(final String format, final Object arg) {

	}

	@Override
	default void warn(final String format, final Object... arguments) {

	}

	@Override
	default void warn(final String format, final Object arg1, final Object arg2) {

	}

	@Override
	default void warn(final String msg, final Throwable t) {

	}

	@Override
	default boolean isWarnEnabled(final Marker marker) {
		return true;
	}

	@Override
	default void warn(final Marker marker, final String msg) {

	}

	@Override
	default void warn(final Marker marker, final String format, final Object arg) {

	}

	@Override
	default void warn(final Marker marker, final String format, final Object arg1,
		final Object arg2) {

	}

	@Override
	default void warn(final Marker marker, final String format, final Object... arguments) {

	}

	@Override
	default void warn(final Marker marker, final String msg, final Throwable t) {

	}

	@Override
	default boolean isErrorEnabled() {
		return true;
	}

	@Override
	default void error(final String msg) {

	}

	@Override
	default void error(final String format, final Object arg) {

	}

	@Override
	default void error(final String format, final Object arg1, final Object arg2) {

	}

	@Override
	default void error(final String format, final Object... arguments) {

	}

	@Override
	default void error(final String msg, final Throwable t) {

	}

	@Override
	default boolean isErrorEnabled(final Marker marker) {
		return true;
	}

	@Override
	default void error(final Marker marker, final String msg) {

	}

	@Override
	default void error(final Marker marker, final String format, final Object arg) {

	}

	@Override
	default void error(final Marker marker, final String format, final Object arg1,
		final Object arg2) {

	}

	@Override
	default void error(final Marker marker, final String format, final Object... arguments) {

	}

	@Override
	default void error(final Marker marker, final String msg, final Throwable t) {

	}

}
