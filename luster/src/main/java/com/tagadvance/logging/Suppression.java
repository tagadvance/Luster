package com.tagadvance.logging;

import static java.util.Objects.requireNonNull;

import com.tagadvance.debounce.Debouncer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;

/**
 * One open coalescing window: the first occurrence has been logged in full, and everything since
 * has been counted rather than logged.
 * <p>
 * Sampled arguments are held by reference, so an argument mutated after it was logged will show
 * its new value in the summary.
 */
final class Suppression {

	private static final int SAMPLE_SIZE = 3;

	private final Fingerprint fingerprint;

	private final Instant firstAt;

	private final AtomicBoolean claimed = new AtomicBoolean();

	private final AtomicLong suppressed = new AtomicLong();

	private final ReentrantLock sampleLock = new ReentrantLock();

	private final List<Object[]> firstSamples = new ArrayList<>(SAMPLE_SIZE);

	private final Deque<Object[]> lastSamples = new ArrayDeque<>(SAMPLE_SIZE);

	private final Debouncer debouncer;

	private @Nullable Marker marker;

	private @Nullable String throwableSummary;

	Suppression(final Fingerprint fingerprint, final Instant firstAt,
		final ScheduledExecutorService scheduler, final Duration quietPeriod,
		final Duration maxWait, final Consumer<Suppression> onWindowClosed) {
		this.fingerprint = requireNonNull(fingerprint, "fingerprint must not be null");
		this.firstAt = requireNonNull(firstAt, "firstAt must not be null");
		requireNonNull(onWindowClosed, "onWindowClosed must not be null");
		this.debouncer = Debouncer.trailing(scheduler, quietPeriod,
			() -> onWindowClosed.accept(this)).withMaxWait(maxWait);
	}

	Fingerprint fingerprint() {
		return fingerprint;
	}

	/**
	 * @return {@literal true} for exactly one caller — the one whose event is logged in full
	 */
	boolean claimFirst(final @Nullable Marker marker, final @Nullable Throwable throwable) {
		if (!claimed.compareAndSet(false, true)) {
			return false;
		}

		this.marker = marker;
		this.throwableSummary = throwable == null ? null : throwable.toString();

		return true;
	}

	void record(final Object @Nullable [] args) {
		suppressed.incrementAndGet();

		final var sample = args == null ? new Object[0] : args;
		sampleLock.lock();
		try {
			if (firstSamples.size() < SAMPLE_SIZE) {
				firstSamples.add(sample);
			}

			if (lastSamples.size() == SAMPLE_SIZE) {
				lastSamples.removeFirst();
			}

			lastSamples.addLast(sample);
		} finally {
			sampleLock.unlock();
		}
	}

	void signal() {
		debouncer.signal();
	}

	/**
	 * Runs the window-closed callback now, on the calling thread.
	 */
	void flush() {
		debouncer.flush();
	}

	/**
	 * @return the arguments for the summary line, or empty if nothing was suppressed
	 */
	Optional<Object[]> summary(final Instant now) {
		final var count = suppressed.get();
		if (count == 0) {
			return Optional.empty();
		}

		return Optional.of(new Object[]{count, fingerprint.format(), Duration.between(firstAt, now),
			firstAt, detail()});
	}

	@Nullable Marker marker() {
		return marker;
	}

	private String detail() {
		final var builder = new StringBuilder();
		sampleLock.lock();
		try {
			if (!firstSamples.isEmpty()) {
				builder.append("; first=").append(render(firstSamples));
			}

			if (lastSamples.size() == SAMPLE_SIZE) {
				builder.append(", last=").append(render(lastSamples));
			}
		} finally {
			sampleLock.unlock();
		}

		if (throwableSummary != null) {
			builder.append("; cause=").append(throwableSummary);
		}

		return builder.toString();
	}

	private static String render(final Iterable<Object[]> samples) {
		final var rendered = new ArrayList<String>();
		samples.forEach(sample -> rendered.add(Arrays.deepToString(sample)));

		return rendered.stream().collect(Collectors.joining(", "));
	}

}
