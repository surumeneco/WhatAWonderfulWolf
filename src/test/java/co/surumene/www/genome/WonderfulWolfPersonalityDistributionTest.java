package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.founder.WonderfulWolfFounderTargetGenerator;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeRandom;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfPersonalityDistributionTest {
    @Test
    void founderClassificationRemainsCloseToTwentyTwoUniformClassificationKeys() {
        WonderfulWolfFounderTargetGenerator generator =
                new WonderfulWolfFounderTargetGenerator(
                        WwwConfigLoader.loadDefaults());
        WonderfulWolfGenomeProfile profile =
                new WonderfulWolfGenomeProfile(
                        WwwConfigLoader.loadDefaults());
        GenomeRandom random = new TestRandom(2026100706L);
        EnumMap<Personality, Integer> counts =
                new EnumMap<>(Personality.class);

        int samples = 20_000;
        for (int sample = 0; sample < samples; sample++) {
            var target = generator.generate(FounderOrigin.NATURAL, random);
            Map<GenomeAddress, AddressAggregate> aggregates =
                    new java.util.HashMap<>();
            for (PersonalityFactor factor : PersonalityFactor.values()) {
                aggregates.put(
                        new GenomeAddress(0x03, factor.targetId()),
                        centered(target.personalityFactors().get(factor)));
            }
            Personality personality = profile.mapPhenotype(
                    new DecodedGenome(aggregates, List.of()))
                    .personality();
            counts.merge(personality, 1, Integer::sum);
        }

        for (Personality personality : Personality.values()) {
            int keyWeight = switch (personality) {
                case HASTY, NAUGHTY, CAUTIOUS, ROWDY, POWERFUL -> 2;
                default -> 1;
            };
            double expected = keyWeight / 22.0;
            double actual = counts.getOrDefault(personality, 0)
                    / (double) samples;
            double tolerance = keyWeight == 2 ? 0.015 : 0.012;
            assertTrue(
                    Math.abs(actual - expected) <= tolerance,
                    () -> personality + " expected=" + expected
                            + " actual=" + actual);
        }
    }

    private static AddressAggregate centered(double score) {
        double positive = Math.max(0.0, 2.0 * score - 1.0);
        double negativeSurvival = score >= 0.5 ? 1.0 : 2.0 * score;
        return new AddressAggregate(
                positive,
                negativeSurvival,
                positive * negativeSurvival,
                List.of());
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
