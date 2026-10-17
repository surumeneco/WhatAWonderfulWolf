package co.surumene.www.runtime;

import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Finds aura sources once per wolf, rather than rescanning expressions per potion. */
final class AuraSelection {
    private AuraSelection() {}

    static Map<Trait, TraitStrength> select(List<ExpressedTrait> expressed) {
        EnumMap<Trait, TraitStrength> selected = new EnumMap<>(Trait.class);
        for (ExpressedTrait entry : expressed) {
            switch (entry.trait()) {
                case MINING_SUPPORT, MUSCLE, GUARDIAN, HEALING ->
                        selected.putIfAbsent(entry.trait(), entry.strength());
                default -> { }
            }
        }
        return selected;
    }
}
