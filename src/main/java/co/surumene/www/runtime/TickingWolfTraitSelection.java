package co.surumene.www.runtime;

import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Trait;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Selects only traits requiring periodic world interactions (not event-only traits). */
public final class TickingWolfTraitSelection {
    private static final Set<Trait> PERIODIC = EnumSet.of(
            Trait.MINING_SUPPORT, Trait.MUSCLE, Trait.GUARDIAN, Trait.HEALING,
            Trait.WATCHMAN, Trait.SCAVENGER, Trait.FIRE_BIRD,
            Trait.UNYIELDING, Trait.SWIMMER, Trait.TARGET);
    private TickingWolfTraitSelection() {}

    public static boolean needsTick(List<ExpressedTrait> expressed) {
        for (ExpressedTrait trait : expressed) {
            if (PERIODIC.contains(trait.trait())) return true;
        }
        return false;
    }
}
