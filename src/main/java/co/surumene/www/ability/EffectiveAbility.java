package co.surumene.www.ability;

import java.util.Objects;

public record EffectiveAbility(
        double baseNormalized,
        double baseCanonical,
        double effectiveNormalized,
        double effectiveCanonical,
        AbilityRank rank,
        boolean injuryActive) {

    public EffectiveAbility {
        if (!Double.isFinite(baseNormalized) || baseNormalized < 0.0
                || !Double.isFinite(baseCanonical) || baseCanonical < 0.0
                || !Double.isFinite(effectiveNormalized) || effectiveNormalized < 0.0
                || !Double.isFinite(effectiveCanonical) || effectiveCanonical < 0.0) {
            throw new IllegalArgumentException("ability values must be finite and >= 0");
        }
        Objects.requireNonNull(rank, "rank");
    }
}
