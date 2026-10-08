package co.surumene.www.command;

import co.surumene.wgl.api.BreedingParentSource;
import co.surumene.wgl.api.GenomeEngine;

import java.util.Base64;
import java.util.Objects;

/** WWW command representation for WGLP parent-source containers. */
public final class OffspringParentSourceCodec {
    private OffspringParentSourceCodec() {}

    public static BreedingParentSource decode(
            String encoded,
            GenomeEngine engine) {
        Objects.requireNonNull(encoded, "encoded");
        Objects.requireNonNull(engine, "engine");
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "parent source must be Base64 encoded WGLP data",
                    error);
        }
        return engine.decodeParentSource(bytes);
    }

    public static String encode(
            BreedingParentSource source,
            GenomeEngine engine) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(engine, "engine");
        return Base64.getEncoder().encodeToString(
                engine.encodeParentSource(source));
    }
}
