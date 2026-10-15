package co.surumene.www.spawn;

import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Objects;

public final class NaturalWolfSpawnPolicy {
    private NaturalWolfSpawnPolicy() {}

    public static boolean shouldConvert(
            CreatureSpawnEvent.SpawnReason reason,
            double probability,
            double draw) {
        Objects.requireNonNull(reason, "reason");
        validate(probability, draw);
        return reason == CreatureSpawnEvent.SpawnReason.NATURAL
                && draw < probability;
    }

    public static boolean shouldConvertGeneratedWolf(
            boolean newChunk,
            double probability,
            double draw) {
        validate(probability, draw);
        return newChunk && draw < probability;
    }

    private static void validate(double probability, double draw) {
        if (!Double.isFinite(probability)
                || probability < 0.0
                || probability > 1.0) {
            throw new IllegalArgumentException(
                    "probability must be finite and within [0,1]");
        }
        if (!Double.isFinite(draw)
                || draw < 0.0
                || draw >= 1.0) {
            throw new IllegalArgumentException(
                    "draw must be finite and within [0,1)");
        }
    }
}
