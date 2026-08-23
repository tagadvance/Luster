package com.tagadvance.mockingbird;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import org.jspecify.annotations.Nullable;

/**
 * The default {@link MimicSerializer}, backed by JSON.
 * <p>
 * The serialization library is deliberately absent from this class's public surface, so it stays
 * an implementation detail rather than something every consumer inherits on their compile
 * classpath. If you need custom type handling, implement {@link MimicSerializer} yourself — it is
 * two methods.
 */
public final class JsonMimicSerializer implements MimicSerializer {

	private final Gson gson = new GsonBuilder().serializeNulls().create();

	@Override
	public void write(final Writer writer, final @Nullable Object value, final Type type) {
		gson.toJson(value, type, writer);
	}

	@Override
	public @Nullable Object read(final Reader reader, final Type type) {
		return gson.fromJson(reader, TypeToken.get(type));
	}

}
