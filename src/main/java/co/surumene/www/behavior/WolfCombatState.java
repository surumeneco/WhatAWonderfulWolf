package co.surumene.www.behavior;

import org.bukkit.entity.Wolf;

public final class WolfCombatState {
    private WolfCombatState() {}
    public static void clear(Wolf wolf) {
        if (wolf.getTarget() != null) wolf.setTarget(null);
        if (wolf.isAngry()) wolf.setAngry(false);
    }
}
