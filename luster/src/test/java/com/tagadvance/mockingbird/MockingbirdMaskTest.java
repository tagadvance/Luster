package com.tagadvance.mockingbird;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A mask works here too: {@code createProxy} takes {@code I extends T}, so the recorded interface
 * may be a sub-interface of anything the instance implements.
 */
class MockingbirdMaskTest {

	@TempDir
	Path path;

	@Test
	void anInterfaceFromADependencyCanBeRecordedThroughAMask() {
		final var impl = new VendorImpl();

		new Mockingbird(path, Mockingbird.Mode.RECORD).createProxy(RecordedVendorApi.class, impl)
			.getTenants();
		assertEquals(1, impl.calls.get());

		final List<String> replayed = new Mockingbird(path, Mockingbird.Mode.REPLAY).createProxy(
			RecordedVendorApi.class, impl).getTenants();

		assertEquals(List.of("acme"), replayed);
		assertEquals(1, impl.calls.get(), "replay must not call through");
	}

	/** Pretend this lives in a dependency. */
	public interface VendorApi {

		List<String> getTenants();

	}

	public interface RecordedVendorApi extends VendorApi {

		@Override
		List<String> getTenants();

	}

	public static final class VendorImpl implements VendorApi {

		final AtomicInteger calls = new AtomicInteger();

		@Override
		public List<String> getTenants() {
			calls.incrementAndGet();

			return List.of("acme");
		}

	}

}
