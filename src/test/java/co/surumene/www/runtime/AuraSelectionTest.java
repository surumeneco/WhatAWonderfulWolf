package co.surumene.www.runtime;

import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AuraSelectionTest {
    @Test void onlyAuraExpressionsRequireNearbyPlayerSearch() {
        assertTrue(AuraSelection.select(List.of()).isEmpty());
        assertTrue(AuraSelection.select(List.of(
                new ExpressedTrait(Trait.WATCHMAN, TraitStrength.STRONG))).isEmpty());
        var selected = AuraSelection.select(List.of(
                new ExpressedTrait(Trait.MINING_SUPPORT, TraitStrength.WEAK),
                new ExpressedTrait(Trait.GUARDIAN, TraitStrength.STRONG),
                new ExpressedTrait(Trait.SCAVENGER, TraitStrength.STRONG)));
        assertEquals(2, selected.size());
        assertEquals(TraitStrength.WEAK, selected.get(Trait.MINING_SUPPORT));
        assertEquals(TraitStrength.STRONG, selected.get(Trait.GUARDIAN));
    }

    @Test void firstExpressionWinsForDuplicateTrait() {
        var selected = AuraSelection.select(List.of(
                new ExpressedTrait(Trait.HEALING, TraitStrength.WEAK),
                new ExpressedTrait(Trait.HEALING, TraitStrength.STRONG)));
        assertEquals(TraitStrength.WEAK, selected.get(Trait.HEALING));
    }
}
