package co.surumene.www.config;

import co.surumene.www.domain.Ability;
import java.util.EnumMap;

public final class DefaultStats {
    private DefaultStats() {}

    public static String key(Ability ability) {
        return switch (ability) {
            case HEALTH -> "max-health";
            case DEFENSE -> "defense";
            case PATIENCE -> "patience";
            case SIZE -> "size";
            case INVENTORY -> "inventory";
            case MOVEMENT_SPEED -> "ground-speed";
            case JUMP -> "jump-strength";
            case STEP_HEIGHT -> "step-height";
            case ATTACK_DAMAGE -> "attack-damage";
            case ATTACK_SPEED -> "attack-speed";
        };
    }

    public static WwwConfig.Stats create() {
        EnumMap<Ability, WwwConfig.Stat> map = new EnumMap<>(Ability.class);
        map.put(Ability.HEALTH, new WwwConfig.Stat(20.0, 60.0, "rank"));
        map.put(Ability.DEFENSE, new WwwConfig.Stat(0.0, 30.0, "rank"));
        map.put(Ability.PATIENCE, new WwwConfig.Stat(0.0, 1.0, "rank"));
        map.put(Ability.SIZE, new WwwConfig.Stat(1.5, 3.0, "rank"));
        map.put(Ability.INVENTORY, new WwwConfig.Stat(0.0, 30.0, "rank"));
        map.put(Ability.MOVEMENT_SPEED, new WwwConfig.Stat(3.0, 24.0, "rank"));
        map.put(Ability.JUMP, new WwwConfig.Stat(1.0, 5.0, "rank"));
        map.put(Ability.STEP_HEIGHT, new WwwConfig.Stat(0.5, 1.5, "rank"));
        map.put(Ability.ATTACK_DAMAGE, new WwwConfig.Stat(1.0, 10.0, "rank"));
        map.put(Ability.ATTACK_SPEED, new WwwConfig.Stat(0.2, 2.0, "rank"));
        return new WwwConfig.Stats(map);
    }
}
