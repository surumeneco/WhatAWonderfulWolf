package co.surumene.www.ability;

import co.surumene.www.domain.Ability;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record EffectiveAbilities(
        Map<Ability, EffectiveAbility> abilities,
        double ageGameDays,
        AgeCurve ageCurve) {

    public EffectiveAbilities {
        Objects.requireNonNull(abilities, "abilities");
        Objects.requireNonNull(ageCurve, "ageCurve");
        if (!Double.isFinite(ageGameDays) || ageGameDays < 0.0) {
            throw new IllegalArgumentException("ageGameDays must be finite and >= 0");
        }

        EnumMap<Ability, EffectiveAbility> copy = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            EffectiveAbility value = abilities.get(ability);
            if (value == null) {
                throw new IllegalArgumentException("abilities is missing " + ability);
            }
            copy.put(ability, value);
        }
        if (copy.size() != abilities.size()) {
            throw new IllegalArgumentException("abilities contains unexpected entries");
        }
        abilities = Collections.unmodifiableMap(copy);
    }

    public EffectiveAbility get(Ability ability) {
        return abilities.get(Objects.requireNonNull(ability, "ability"));
    }

    public double canonical(Ability ability) {
        return get(ability).effectiveCanonical();
    }
}
