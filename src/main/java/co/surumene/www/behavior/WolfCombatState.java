package co.surumene.www.behavior;

import org.bukkit.entity.Wolf;

public final class WolfCombatState {
    private WolfCombatState() {}
    public static void clear(Wolf wolf) {
        wolf.setTarget(null);
        wolf.setAngry(false);
    }
}
