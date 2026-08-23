package com.tagadvance.mockingbird;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import org.jspecify.annotations.Nullable;

/**
 * Reads and writes recorded values.
 * <p>
 * Implementations are handed the method's <em>declared</em> {@link Type}, generics included, so a
 * {@code List<Tenant>} comes back as a list of {@code Tenant} rather than of whatever the
 * serializer's default mapping produces.
 */
public interface MimicSerializer {

	/**
	 * @param writer the destination
	 * @param value  the value to write, possibly {@literal null}
	 * @param type   the declared type of {@literal value}
	 * @throws IOException if writing fails
	 */
	void write(Writer writer, @Nullable Object value, Type type) throws IOException;

	/**
	 * @param reader the source
	 * @param type   the declared type to read as
	 * @return the value, possibly {@literal null}
	 * @throws IOException if reading fails
	 */
	@Nullable Object read(Reader reader, Type type) throws IOException;

}
