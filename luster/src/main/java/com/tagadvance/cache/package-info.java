/**
 * Declares caching on an interface and applies it with a proxy, so the implementation stays
 * ignorant of it.
 *
 * <h2>Masking an interface you do not own</h2>
 *
 * {@link com.tagadvance.cache.CacheConfiguration} has to be on the interface being proxied, which
 * is a problem when that interface belongs to a dependency. A <em>mask</em> is a sub-interface
 * that redeclares the methods you want cached and carries the annotations:
 *
 * <pre>{@code
 * public interface CachedTenantApi extends com.vendor.TenantApi {
 *
 * 	@Override
 * 	@CacheConfiguration(name = "tenants", expireAfterWrite = "PT5M")
 * 	List<Tenant> getTenants();
 *
 * }
 *
 * final TenantApi api = factory.newCache(CachedTenantApi.class, vendorImpl).proxy();
 * }</pre>
 *
 * <p>{@code newCache} takes {@code I extends T}, so the instance only has to implement the vendor
 * interface — it need not implement the mask. {@link com.tagadvance.proxy.Invocation#resolve}
 * bridges each masked method to the instance's own implementation.
 *
 * <p><strong>Keep the {@code @Override}.</strong> It is what makes the mask checked: if the
 * dependency renames or changes the signature of a masked method, the mask stops compiling. A
 * verbatim copy of the interface would work at runtime but is linked to the original by nothing,
 * so it would drift silently — the failure mode that makes XML descriptors such as {@code orm.xml}
 * and IDE external-annotation files hard to keep honest. Omitting {@code @Override} still fails,
 * but only when the proxy is created rather than when the code is compiled.
 *
 * <p>A mask cannot be applied to a {@code sealed} interface, or to a class — {@link java.lang.reflect.Proxy}
 * only proxies interfaces.
 */
@NullMarked
package com.tagadvance.cache;

import org.jspecify.annotations.NullMarked;
