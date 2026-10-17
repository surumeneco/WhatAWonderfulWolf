package co.surumene.www.runtime;

import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TickingWolfTraitSelectionTest {
    private static ExpressedTrait trait(Trait name) {
        return new ExpressedTrait(name, TraitStrength.WEAK);
    }

    @Test void pureBreedingAndHitOnlyTraitsRequireNoPeriodicWork() {
        assertFalse(TickingWolfTraitSelection.needsTick(List.of()));
        assertFalse(TickingWolfTraitSelection.needsTick(
                List.of(trait(Trait.DIRECT_INHERITANCE), trait(Trait.LIFE_DRAIN))));
    }

    @Test void aurasWatchmanScavengerAndSelfBuffRequirePeriodicWork() {
        for (Trait trait : List.of(Trait.MINING_SUPPORT, Trait.MUSCLE,
                Trait.GUARDIAN, Trait.HEALING, Trait.WATCHMAN, Trait.SCAVENGER,
                Trait.FIRE_BIRD, Trait.UNYIELDING, Trait.SWIMMER, Trait.TARGET)) {
            assertTrue(TickingWolfTraitSelection.needsTick(List.of(trait(trait))),
                    "should tick: " + trait);
        }
    }
}
