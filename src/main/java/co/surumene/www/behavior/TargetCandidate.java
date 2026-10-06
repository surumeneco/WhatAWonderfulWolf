package co.surumene.www.behavior;

import java.util.Objects;
import java.util.UUID;

public record TargetCandidate(
        UUID targetId,
        TargetSource source,
        boolean withinActionDistance,
        double wolfDistanceSquared,
        double referenceDistanceSquared) {

    public TargetCandidate {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(source, "source");
        if (!Double.isFinite(wolfDistanceSquared)
                || wolfDistanceSquared < 0.0
                || !Double.isFinite(referenceDistanceSquared)
                || referenceDistanceSquared < 0.0) {
            throw new IllegalArgumentException(
                    "target distances must be finite and >= 0");
        }
    }
}
