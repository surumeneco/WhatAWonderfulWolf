package co.surumene.www.individual;

import java.util.Objects;
import java.util.UUID;

public record WorldPosition(UUID worldId, double x, double y, double z) {
    public WorldPosition {
        Objects.requireNonNull(worldId, "worldId");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("coordinates must be finite");
        }
    }
}
