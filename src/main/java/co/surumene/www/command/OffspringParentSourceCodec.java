package co.surumene.www.command;

import co.surumene.wgl.api.BreedingParentSource;
import co.surumene.wgl.api.GenomeEngine;

import java.util.Base64;
import java.util.Objects;

/** WWW command representation for WGLP parent-source containers. */
public final class OffspringParentSourceCodec {
    private static final String TOKEN_PREFIX = "wglp_";
    private static final int MAX_SOURCE_BYTES = 1_048_576;

    private OffspringParentSourceCodec() {}

    public static String encodeToken(BreedingParentSource source, GenomeEngine engine) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(engine, "engine");
        byte[] binary = engine.encodeParentSource(source);
        if (binary.length > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("parent source exceeds 1 MiB");
        }
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(binary);
    }

    public static BreedingParentSource decodeToken(String token, GenomeEngine engine) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(engine, "engine");
        if (!token.startsWith(TOKEN_PREFIX)) {
            throw new IllegalArgumentException("parent source must start with wglp_");
        }
        String value = token.substring(TOKEN_PREFIX.length());
        if (value.isEmpty() || value.length() > 1_398_108
                || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("invalid WGLP token");
        }
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        if (bytes.length > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("parent source exceeds 1 MiB");
        }
        return engine.decodeParentSource(bytes);
    }

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
