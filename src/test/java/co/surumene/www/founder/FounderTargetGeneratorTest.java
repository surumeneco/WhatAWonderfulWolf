package co.surumene.www.founder;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.TraitStrength;
import co.surumene.wgl.api.GenomeRandom;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FounderTargetGeneratorTest {
    private final WonderfulWolfFounderTargetGenerator generator =
            new WonderfulWolfFounderTargetGenerator(WwwConfigLoader.loadDefaults());

    @Test
    void naturalTargetsStayInsideNaturalFounderRanges() {
        GenomeRandom random = new TestRandom(20261005L);
        for (int sample = 0; sample < 1_000; sample++) {
            FounderTarget target = generator.generate(FounderOrigin.NATURAL, random);
            assertEquals(FounderOrigin.NATURAL, target.origin());
            for (Ability ability : Ability.values()) {
                assertRange(target.abilities().get(ability), 0.0, 0.5);
            }
            assertCommonRanges(target);
        }
    }

    @Test
    void wolfTrapTargetsStayInsideWolfTrapFounderRanges() {
        GenomeRandom random = new TestRandom(20261006L);
        for (int sample = 0; sample < 1_000; sample++) {
            FounderTarget target = generator.generate(FounderOrigin.WOLF_TRAP, random);
            assertEquals(FounderOrigin.WOLF_TRAP, target.origin());
            for (Ability ability : Ability.values()) {
                assertRange(target.abilities().get(ability), 0.5, 1.5);
            }
            assertCommonRanges(target);
        }
    }

    @Test
    void sameRandomSeedProducesSameFounderTarget() {
        assertEquals(
                generator.generate(FounderOrigin.WOLF_TRAP, new TestRandom(123456789L)),
                generator.generate(FounderOrigin.WOLF_TRAP, new TestRandom(123456789L)));
    }

    @Test
    void traitDrawProducesOnlyTheSpecifiedFounderStates() {
        GenomeRandom random = new TestRandom(314159265L);
        for (int sample = 0; sample < 5_000; sample++) {
            FounderTarget target = generator.generate(FounderOrigin.NATURAL, random);
            assertTrue(target.traits().size() <= 2);
            assertEquals(
                    target.traits().size(),
                    new HashSet<>(target.traits().stream().map(ExpressedTrait::trait).toList()).size());
            if (target.traits().size() == 2) {
                assertTrue(target.traits().stream().allMatch(t -> t.strength() == TraitStrength.WEAK));
            }
        }
    }

    private static void assertCommonRanges(FounderTarget target) {
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            assertRange(target.personalityFactors().get(factor), 0.0, 1.0);
        }
        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            assertRange(target.developmentFactors().get(factor), 0.0, 1.0);
        }
        assertRange(target.relationship().initialAffinityScore(), 0.0, 1.0);
        assertRange(target.relationship().affinityChangeScore(), 0.0, 1.0);
    }

    private static void assertRange(double value, double min, double max) {
        assertTrue(Double.isFinite(value) && value >= min && value <= max,
                () -> value + " outside [" + min + ", " + max + "]");
    }

    private static final class TestRandom implements GenomeRandom {
        private final SplittableRandom random;

        private TestRandom(long seed) {
            random = new SplittableRandom(seed);
        }

        @Override public long nextLong() { return random.nextLong(); }
        @Override public double nextDouble() { return random.nextDouble(); }
        @Override public int nextInt(int bound) { return random.nextInt(bound); }
        @Override public boolean nextBoolean() { return random.nextBoolean(); }
    }
}
