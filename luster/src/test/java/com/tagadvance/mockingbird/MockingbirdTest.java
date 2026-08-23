package com.tagadvance.mockingbird;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Mockingbird}.
 */
class MockingbirdTest {

	@TempDir
	Path path;

	@Test
	void aRecordingIsReplayedByALaterInstance() {
		final var upstream = new CountingApi();

		record(upstream).greeting();
		assertEquals(1, upstream.calls.get());

		// a fresh Mockingbird, with a fresh counter, the way a later JVM run would see it
		final var replayed = new Mockingbird(path, Mockingbird.Mode.REPLAY).createProxy(Api.class,
			upstream);

		assertEquals("hello", replayed.greeting());
		assertEquals(1, upstream.calls.get(), "replay must not call through");
	}

	@Test
	void recordingsAreGroupedInADirectoryPerInterface() throws IOException {
		record(new CountingApi()).greeting();

		final var directory = path.resolve(Api.class.getName());

		assertTrue(Files.isDirectory(directory), "expected a directory named for the interface");
		try (final var files = Files.list(directory)) {
			assertEquals(1, files.count());
		}
	}

	@Test
	void aGenericReturnTypeSurvivesTheRoundTrip() {
		final var upstream = new CountingApi();
		record(upstream).tenants();

		final List<Tenant> replayed = replay(upstream).tenants();

		assertEquals(2, replayed.size());
		// the bug this pins: the recorded concrete class was ArrayList, so replay yielded
		// LinkedTreeMap and the first cast at the call site threw
		assertEquals("acme", replayed.get(0).name());
	}

	@Test
	void aGenericMapReturnTypeSurvivesTheRoundTrip() {
		final var upstream = new CountingApi();
		record(upstream).quotas();

		final Map<String, Integer> replayed = replay(upstream).quotas();

		assertEquals(7, replayed.get("acme"));
	}

	@Test
	void aNullReturnIsRecorded() {
		final var upstream = new CountingApi();

		assertNull(record(upstream).missing());
		assertNull(replay(upstream).missing());
		assertEquals(1, upstream.calls.get(), "replay must not call through");
	}

	@Test
	void aVoidReturnIsRecorded() {
		final var upstream = new CountingApi();

		record(upstream).touch();
		replay(upstream).touch();

		assertEquals(1, upstream.calls.get(), "replay must not call through");
	}

	@Test
	void aFailureIsRecordedAndRethrownOnReplay() {
		final var upstream = new CountingApi();

		final var recorded = assertThrows(IllegalStateException.class,
			() -> record(upstream).broken());
		assertEquals("upstream is down", recorded.getMessage());

		final var replayed = assertThrows(IllegalStateException.class,
			() -> replay(upstream).broken());

		assertEquals("upstream is down", replayed.getMessage());
		assertEquals(1, upstream.calls.get(), "a recorded failure must not call through again");
	}

	@Test
	void replayModeFailsLoudlyWhenThereIsNoRecording() {
		final var proxy = new Mockingbird(path, Mockingbird.Mode.REPLAY).createProxy(Api.class,
			new CountingApi());

		final var e = assertThrows(MimicReplayException.class, proxy::greeting);

		assertTrue(e.getMessage().contains("no recording"), e.getMessage());
	}

	@Test
	void recordModeAlwaysCallsThrough() {
		final var upstream = new CountingApi();

		record(upstream).greeting();
		record(upstream).greeting();

		assertEquals(2, upstream.calls.get());
	}

	@Test
	void differentArgumentsResolveToDifferentRecordings() throws IOException {
		final var upstream = new CountingApi();
		final var proxy = new Mockingbird(path, Mockingbird.Mode.AUTO).createProxy(Api.class,
			upstream);

		proxy.echo("one");
		proxy.echo("two");

		try (final var files = Files.list(path.resolve(Api.class.getName()))) {
			assertEquals(2, files.count(), "distinct arguments must not share a recording");
		}
	}

	@Test
	void equalArgumentsResolveToTheSameRecording() {
		final var upstream = new CountingApi();

		assertEquals("tenant-1", record(upstream).fetch(new Request("tenant-1")));
		assertEquals("tenant-1", replay(upstream).fetch(new Request("tenant-1")));

		assertEquals(1, upstream.calls.get(),
			"a distinct but equal argument must find the recording");
	}

	private Api record(final Api upstream) {
		return new Mockingbird(path, Mockingbird.Mode.RECORD).createProxy(Api.class, upstream);
	}

	private Api replay(final Api upstream) {
		return new Mockingbird(path, Mockingbird.Mode.REPLAY).createProxy(Api.class, upstream);
	}

	public interface Api {

		String greeting();

		String echo(String value);

		String fetch(Request request);

		List<Tenant> tenants();

		Map<String, Integer> quotas();

		String missing();

		void touch();

		String broken();

	}

	public record Tenant(String name) {

	}

	/**
	 * Overrides neither {@code equals} nor {@code hashCode}, like most request POJOs.
	 */
	public static final class Request {

		private final String tenant;

		public Request(final String tenant) {
			this.tenant = tenant;
		}

	}

	public static final class CountingApi implements Api {

		final AtomicInteger calls = new AtomicInteger();

		@Override
		public String greeting() {
			calls.incrementAndGet();

			return "hello";
		}

		@Override
		public String echo(final String value) {
			calls.incrementAndGet();

			return value;
		}

		@Override
		public String fetch(final Request request) {
			calls.incrementAndGet();

			return request.tenant;
		}

		@Override
		public List<Tenant> tenants() {
			calls.incrementAndGet();

			return List.of(new Tenant("acme"), new Tenant("globex"));
		}

		@Override
		public Map<String, Integer> quotas() {
			calls.incrementAndGet();

			return Map.of("acme", 7);
		}

		@Override
		public String missing() {
			calls.incrementAndGet();

			return null;
		}

		@Override
		public void touch() {
			calls.incrementAndGet();
		}

		@Override
		public String broken() {
			calls.incrementAndGet();

			throw new IllegalStateException("upstream is down");
		}

	}

}
