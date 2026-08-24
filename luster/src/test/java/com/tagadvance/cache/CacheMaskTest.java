package com.tagadvance.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tagadvance.proxy.ProxyInvocationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * A <em>mask</em> is a sub-interface that adds annotations to an interface owned by a dependency,
 * which cannot be annotated in place.
 */
class CacheMaskTest {

	/** Pretend this lives in a dependency and cannot be annotated. */
	public interface VendorApi {

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

	/** The mask: extends the vendor type, redeclares with @Override, adds configuration. */
	public interface CachedVendorApi extends VendorApi {

		@Override
		@CacheConfiguration(name = "tenants", expireAfterWrite = "PT5M")
		List<String> getTenants();

	}

	/** A mask that has drifted -- the method does not exist on the instance. */
	public interface DriftedMask extends VendorApi {

		@CacheConfiguration(name = "gone")
		List<String> methodTheVendorRenamed();

	}

	@Test
	void aMaskAppliesConfigurationToAnUnmodifiableInterface() {
		final var impl = new VendorImpl();
		final VendorApi proxy = new DefaultCacheFactory().newCache(CachedVendorApi.class, impl)
			.proxy();

		final var first = proxy.getTenants();
		final var second = proxy.getTenants();

		assertEquals(1, impl.calls.get(), "the second call should have been served from the cache");
		assertSame(first, second);
	}

	/**
	 * {@literal @Override} on the redeclaration makes a vendor rename a compile error. This is the
	 * runtime backstop for when it was omitted.
	 */
	@Test
	void aDriftedMaskFailsWhenTheProxyIsCreated() {
		final var e = assertThrows(ProxyInvocationException.class,
			() -> new DefaultCacheFactory().newCache(DriftedMask.class, new VendorImpl()));

		assertTrue(e.getMessage().contains("methodTheVendorRenamed"), e.getMessage());
	}

}
