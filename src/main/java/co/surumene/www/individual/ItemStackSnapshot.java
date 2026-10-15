package co.surumene.www.individual;

import java.util.Arrays;
import java.util.Objects;

public final class ItemStackSnapshot {
    private final byte[] bytes;

    public ItemStackSnapshot(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) {
            throw new IllegalArgumentException("serialized item stack must not be empty");
        }
        this.bytes = bytes.clone();
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemStackSnapshot that && Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
