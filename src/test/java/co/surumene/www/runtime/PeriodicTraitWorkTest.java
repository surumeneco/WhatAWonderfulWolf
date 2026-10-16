package co.surumene.www.runtime;

import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PeriodicTraitWorkTest {
    private ExpressedTrait has(Trait kind) {
        return new ExpressedTrait(kind, TraitStrength.STRONG);
    }

    @Test void ignoresNonPeriodicTraits() {
        assertEquals(0, PeriodicTraitWork.from(List.of()));
        assertEquals(0, PeriodicTraitWork.from(List.of(has(Trait.HOLY_POISON))));
    }

    @Test void dispatchesOnlyExpressedCategories() {
        assertEquals(PeriodicTraitWork.STATUS, PeriodicTraitWork.from(List.of(has(Trait.FIRE_BIRD))));
        assertEquals(PeriodicTraitWork.WATCH, PeriodicTraitWork.from(List.of(has(Trait.TARGET))));
        assertEquals(PeriodicTraitWork.SCAVENGE, PeriodicTraitWork.from(List.of(has(Trait.SCAVENGER))));
        assertEquals(PeriodicTraitWork.STATUS | PeriodicTraitWork.WATCH | PeriodicTraitWork.SCAVENGE,
                PeriodicTraitWork.from(List.of(has(Trait.HEALING),has(Trait.WATCHMAN),has(Trait.SCAVENGER))));
    }

    @Test void includesAllPotionTraitKinds() {
        for (Trait trait : List.of(Trait.MINING_SUPPORT,Trait.MUSCLE,Trait.GUARDIAN,
                Trait.HEALING,Trait.FIRE_BIRD,Trait.UNYIELDING,Trait.SWIMMER)) {
            assertTrue(PeriodicTraitWork.has(PeriodicTraitWork.from(List.of(has(trait))),PeriodicTraitWork.STATUS));
        }
    }
}
