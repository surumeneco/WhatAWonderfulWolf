package co.surumene.www.runtime;

import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.domain.Ability;

import java.util.Objects;

public final class NonAttributeAbilityAdapter {
    private NonAttributeAbilityAdapter() {}

    public static double patience(EffectiveAbilities abilities) {
        return Objects.requireNonNull(abilities, "abilities")
                .canonical(Ability.PATIENCE);
    }

    public static double retreatHealthFraction(EffectiveAbilities abilities) {
        return 1.0 - patience(abilities);
    }

    public static int inventorySlots(EffectiveAbilities abilities) {
        return (int) Math.round(
                Objects.requireNonNull(abilities, "abilities")
                        .canonical(Ability.INVENTORY));
    }
}
