package co.surumene.www.ability;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.*;

import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;

public final class EffectiveAbilityPipeline {
    private static final double NORMAL_RANK_COUNT = 9.0;

    private EffectiveAbilityPipeline() {}

    public static EffectiveAbilities evaluate(
            PhenotypeSnapshot snapshot,
            double ageGameDays,
            WwwConfig.Runtime runtime) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(runtime, "runtime");
        if (!Double.isFinite(ageGameDays) || ageGameDays < 0.0) {
            throw new IllegalArgumentException("ageGameDays must be finite and >= 0");
        }

        AgeCurve curve = AgeCurve.from(snapshot, runtime.age());
        double ageRankReduction = curve.rankReductionAt(ageGameDays);
        EnumMap<Ability, EffectiveAbility> values = new EnumMap<>(Ability.class);

        for (Ability ability : Ability.values()) {
            double baseNormalized = snapshot.abilities().get(ability);
            double ageReduction =
                    ageRankReduction * ageSensitivity(runtime.age().sensitivity(), ability)
                            / NORMAL_RANK_COUNT;

            Optional<InjuryPhenotype> injury = snapshot.injuries().stream()
                    .filter(candidate -> candidate.ability() == ability)
                    .findFirst();
            boolean injuryActive =
                    injury.filter(candidate -> ageGameDays >= candidate.onsetGameDay())
                            .isPresent();
            double injuryReduction = injuryActive
                    ? injury.orElseThrow().severityRank() / NORMAL_RANK_COUNT
                    : 0.0;

            double effectiveNormalized =
                    Math.max(0.0, baseNormalized - ageReduction - injuryReduction);
            double baseCanonical = AbilityScale.toCanonical(ability, baseNormalized);
            double runtimeCanonical = AbilityScale.toCanonical(ability, effectiveNormalized);

            double personalityMultiplier = PersonalityAbilityModifier.multiplier(
                    snapshot.personality(),
                    ability,
                    snapshot.expressedTraits(),
                    runtime.personality().modifierRate());
            double effectiveCanonical = AbilityScale.finalizeEffective(
                    ability,
                    runtimeCanonical * personalityMultiplier);

            values.put(
                    ability,
                    new EffectiveAbility(
                            baseNormalized,
                            baseCanonical,
                            effectiveNormalized,
                            effectiveCanonical,
                            AbilityRank.fromNormalized(baseNormalized),
                            injuryActive));
        }

        return new EffectiveAbilities(values, ageGameDays, curve);
    }

    private static double ageSensitivity(
            WwwConfig.AgeSensitivity sensitivity,
            Ability ability) {
        return switch (ability) {
            case HEALTH -> sensitivity.health();
            case DEFENSE -> sensitivity.defense();
            case PATIENCE -> sensitivity.patience();
            case SIZE -> sensitivity.size();
            case INVENTORY -> sensitivity.inventory();
            case MOVEMENT_SPEED -> sensitivity.movementSpeed();
            case JUMP -> sensitivity.jump();
            case STEP_HEIGHT -> sensitivity.stepHeight();
            case ATTACK_DAMAGE -> sensitivity.attackDamage();
            case ATTACK_SPEED -> sensitivity.attackSpeed();
        };
    }
}
