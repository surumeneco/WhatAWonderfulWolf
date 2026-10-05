package co.surumene.www.individual;

import java.util.Objects;

public record AncestorSnapshot(String displayName, int generation, String lineageId) {
    public AncestorSnapshot {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(lineageId, "lineageId");
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (generation < 0) throw new IllegalArgumentException("generation must be >= 0");
        if (lineageId.isBlank()) throw new IllegalArgumentException("lineageId must not be blank");
    }
}
