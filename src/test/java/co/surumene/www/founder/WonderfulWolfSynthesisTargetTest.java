package co.surumene.www.founder;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeRandom;
import co.surumene.wgl.api.SynthesisAddressPlan;
import co.surumene.wgl.api.SynthesisContext;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfSynthesisTargetTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();
    private final WonderfulWolfGenomeProfile profile = new WonderfulWolfGenomeProfile(config);

    @Test
    void naturalTargetNeverCreatesFounderExtraordinaryAddress() {
        FounderTarget founder = target(FounderOrigin.NATURAL, 0.45);
        WonderfulWolfSynthesisTarget target =
                WonderfulWolfSynthesisTarget.from(founder, config, new FixedRandom(0.5));

        for (Ability ability : Ability.values()) {
            assertEquals(0.45,
                    target.continuousTargets().get(new GenomeAddress(0x00, ability.targetId())),
                    1.0e-12);
            assertFalse(target.continuousTargets().containsKey(
                    new GenomeAddress(0x07, ability.targetId())));
        }
    }

    @Test
    void wolfTrapAbilityAboveOneSplitsIntoBaseAndExtraordinaryWithoutChangingFinalTarget() {
        FounderTarget founder = target(FounderOrigin.WOLF_TRAP, 1.20);
        WonderfulWolfSynthesisTarget target =
                WonderfulWolfSynthesisTarget.from(founder, config, new FixedRandom(0.5));

        for (Ability ability : Ability.values()) {
            double base = target.baseAbilityTargets().get(ability);
            double extraordinary = target.extraordinaryTargets().get(ability);
            assertTrue(base >= 0.85 && base <= 1.0);
            assertTrue(extraordinary > 0.0 && extraordinary <= 0.5);
            assertEquals(1.20, base + extraordinary, 1.0e-12);
            assertEquals(base,
                    target.continuousTargets().get(new GenomeAddress(0x00, ability.targetId())),
                    1.0e-12);
            assertFalse(target.continuousTargets().containsKey(
                    new GenomeAddress(0x07, ability.targetId())));
        }
    }

    @Test
    void centeredAndBoundedAddressesUseTheirConfiguredSynthesisPlans() {
        SynthesisContext context = new SynthesisContext(1, 1, 0.0, 0.0, 1);
        GenomeRandom random = new FixedRandom(0.5);

        SynthesisAddressPlan ability = profile.synthesisPlan(
                new GenomeAddress(0x00, 0x00), 0.40, context, random);
        assertEquals(ability.minPositiveGenes(), ability.maxPositiveGenes());
        assertEquals(ability.minNegativeGenes(), ability.maxNegativeGenes());
        assertTrue(ability.minPositiveGenes() + ability.minNegativeGenes() >= 16);
        assertTrue(ability.maxPositiveGenes() + ability.maxNegativeGenes() <= 24);
        assertEquals(0.40 / 0.725, ability.positiveSaturation(), 1.0e-12);
        assertEquals(0.275, ability.negativeSaturation(), 1.0e-12);

        SynthesisAddressPlan personality = profile.synthesisPlan(
                new GenomeAddress(0x03, 0x00), 0.25, context, random);
        assertEquals(0.0, personality.positiveSaturation(), 1.0e-12);
        assertEquals(0.50, personality.negativeSaturation(), 1.0e-12);
        assertEquals(personality.minNegativeGenes(), personality.maxNegativeGenes());
        assertTrue(personality.minNegativeGenes() >= 8);
        assertTrue(personality.maxNegativeGenes() <= 16);

        SynthesisAddressPlan extraordinary = profile.synthesisPlan(
                new GenomeAddress(0x07, 0x00), 0.40, context, random);
        assertEquals(extraordinary.minPositiveGenes(), extraordinary.maxPositiveGenes());
        assertTrue(extraordinary.minPositiveGenes() >= 8);
        assertTrue(extraordinary.maxPositiveGenes() <= 12);
    }

    @Test
    void traitStateIsRepresentedByDecoderCompatibleLatentScores() {
        FounderTarget founder = target(
                FounderOrigin.NATURAL,
                0.40,
                List.of(
                        new ExpressedTrait(Trait.WATCHMAN, TraitStrength.WEAK),
                        new ExpressedTrait(Trait.MUSCLE, TraitStrength.WEAK)));
        WonderfulWolfSynthesisTarget target =
                WonderfulWolfSynthesisTarget.from(founder, config, new FixedRandom(0.5));

        double watchman = target.continuousTargets().get(new GenomeAddress(0x04, Trait.WATCHMAN.targetId()));
        double muscle = target.continuousTargets().get(new GenomeAddress(0x04, Trait.MUSCLE.targetId()));
        double other = target.continuousTargets().get(new GenomeAddress(0x04, Trait.GUARDIAN.targetId()));

        assertTrue(watchman >= config.genomeProfile().decoder().trait().expressionThreshold());
        assertEquals(watchman, muscle, 1.0e-12);
        assertTrue(other < config.genomeProfile().decoder().trait().expressionThreshold());
    }

    private static FounderTarget target(FounderOrigin origin, double ability) {
        return target(origin, ability, List.of());
    }

    private static FounderTarget target(
            FounderOrigin origin, double ability, List<ExpressedTrait> traits) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability value : Ability.values()) abilities.put(value, ability);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor value : PersonalityFactor.values()) personality.put(value, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor value : DevelopmentFactor.values()) development.put(value, 0.5);
        return new FounderTarget(
                origin,
                abilities,
                personality,
                traits,
                development,
                new FounderRelationshipTarget(0.5, 0.5));
    }

    private static final class FixedRandom implements GenomeRandom {
        private final double value;
        private FixedRandom(double value) { this.value = value; }
        @Override public long nextLong() { return 0L; }
        @Override public double nextDouble() { return value; }
        @Override public int nextInt(int bound) { return 0; }
        @Override public boolean nextBoolean() { return false; }
    }
}
