package co.surumene.www.ability;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.*;
import co.surumene.wgl.api.DecoderIdentity;
import co.surumene.wgl.api.ProfileDescriptor;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class EffectiveAbilityPipelineTest {
    private final WwwConfig.Runtime runtime = WwwConfigLoader.loadDefaults().runtime();

    @Test
    void neutralDevelopmentProducesDocumentedAgeBoundaries() {
        PhenotypeSnapshot snapshot = snapshot(
                Personality.SERIOUS,
                List.of(),
                List.of(),
                0.5);

        AgeCurve curve = AgeCurve.from(snapshot, runtime.age());

        assertEquals(672.0, curve.growthEndGameDay(), 1.0e-9);
        assertEquals(0.5, curve.juvenileSuppressionRank(), 1.0e-9);
        assertEquals(3552.0, curve.agingStartGameDay(), 1.0e-9);
        assertEquals(7872.0, curve.elderGameDay(), 1.0e-9);
        assertEquals(1.5, curve.maximumAgingReductionRank(), 1.0e-9);

        assertEquals(0.5, curve.rankReductionAt(0.0), 1.0e-9);
        assertEquals(0.0, curve.rankReductionAt(672.0), 1.0e-9);
        assertEquals(0.0, curve.rankReductionAt(3552.0), 1.0e-9);
        assertEquals(0.75, curve.rankReductionAt((3552.0 + 7872.0) / 2.0), 1.0e-9);
        assertEquals(1.5, curve.rankReductionAt(7872.0), 1.0e-9);
    }

    @Test
    void personalityAndQuirkBoostModifyOnlyEffectiveCanonicalValue() {
        PhenotypeSnapshot snapshot = snapshot(
                Personality.GENTLE,
                List.of(new ExpressedTrait(Trait.QUIRK_BOOST, TraitStrength.STRONG)),
                List.of(),
                0.5);

        EffectiveAbilities result =
                EffectiveAbilityPipeline.evaluate(snapshot, 672.0, runtime);

        EffectiveAbility health = result.get(Ability.HEALTH);
        EffectiveAbility attackSpeed = result.get(Ability.ATTACK_SPEED);

        assertEquals(0.5, health.baseNormalized(), 1.0e-12);
        assertEquals(40.0, health.baseCanonical(), 1.0e-12);
        assertEquals(46.0, health.effectiveCanonical(), 1.0e-12);
        assertEquals(AbilityRank.COMMON, health.rank());

        assertEquals(1.1, attackSpeed.baseCanonical(), 1.0e-12);
        assertEquals(0.935, attackSpeed.effectiveCanonical(), 1.0e-12);
        assertEquals(0.5, snapshot.abilities().get(Ability.HEALTH), 1.0e-12);
    }

    @Test
    void ageAndManifestedInjuryReduceNormalizedRuntimeValueWithoutChangingSnapshot() {
        PhenotypeSnapshot snapshot = snapshot(
                Personality.SERIOUS,
                List.of(),
                List.of(new InjuryPhenotype(Ability.MOVEMENT_SPEED, 100.0, 4.5)),
                0.5);

        EffectiveAbility before =
                EffectiveAbilityPipeline.evaluate(snapshot, 99.999, runtime)
                        .get(Ability.MOVEMENT_SPEED);
        EffectiveAbility after =
                EffectiveAbilityPipeline.evaluate(snapshot, 100.0, runtime)
                        .get(Ability.MOVEMENT_SPEED);

        assertFalse(before.injuryActive());
        assertEquals(0.5, before.effectiveNormalized(), 1.0e-9);
        assertTrue(after.injuryActive());
        assertEquals(0.0, after.effectiveNormalized(), 1.0e-9);
        assertEquals(0.5, snapshot.abilities().get(Ability.MOVEMENT_SPEED), 1.0e-12);
    }

    @Test
    void ageSensitivityIsPerAbilityAndSizeAndInventoryRemainUnaffectedByDefault() {
        PhenotypeSnapshot snapshot = snapshot(
                Personality.SERIOUS,
                List.of(),
                List.of(),
                0.5);

        EffectiveAbilities elder =
                EffectiveAbilityPipeline.evaluate(snapshot, 9000.0, runtime);

        assertEquals(0.5, elder.get(Ability.SIZE).effectiveNormalized(), 1.0e-12);
        assertEquals(0.5, elder.get(Ability.INVENTORY).effectiveNormalized(), 1.0e-12);
        assertEquals(
                0.5 - 1.5 / 9.0,
                elder.get(Ability.MOVEMENT_SPEED).effectiveNormalized(),
                1.0e-12);
    }

    private static PhenotypeSnapshot snapshot(
            Personality personality,
            List<ExpressedTrait> traits,
            List<InjuryPhenotype> injuries,
            double developmentScore) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 0.5);

        EnumMap<PersonalityFactor, Double> factors = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) factors.put(factor, 0.5);

        EnumMap<DevelopmentFactor, Double> development =
                new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            development.put(factor, developmentScore);
        }

        return new PhenotypeSnapshot(
                new DecoderIdentity(
                        1,
                        new byte[32],
                        new ProfileDescriptor("wonderful-wolf", 1, new byte[32])),
                abilities,
                new RelationshipPerformance(0L, 1L),
                factors,
                personality,
                traits,
                development,
                injuries,
                0.0,
                false);
    }
}
