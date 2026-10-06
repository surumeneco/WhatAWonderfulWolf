package co.surumene.www.behavior;

import org.bukkit.entity.EntityType;

import java.util.EnumSet;
import java.util.Objects;

public final class ActiveThreatPolicy {
    private static final EnumSet<EntityType> CONDITIONALLY_NEUTRAL_ENEMIES =
            EnumSet.of(
                    EntityType.ENDERMAN,
                    EntityType.PIGLIN,
                    EntityType.ZOMBIFIED_PIGLIN,
                    EntityType.SPIDER);

    private ActiveThreatPolicy() {}

    public static boolean isActiveThreat(
            EntityType type,
            boolean enemy,
            boolean targetingCommandChain) {
        Objects.requireNonNull(type, "type");
        if (CONDITIONALLY_NEUTRAL_ENEMIES.contains(type)) {
            return targetingCommandChain;
        }
        return enemy || targetingCommandChain;
    }
}
