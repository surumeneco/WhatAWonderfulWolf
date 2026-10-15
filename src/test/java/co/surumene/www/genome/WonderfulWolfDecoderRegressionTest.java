package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.DecoderIdentity;
import co.surumene.wgl.api.EffectiveContribution;
import co.surumene.wgl.api.GenomeAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfDecoderRegressionTest {
    private static final double EPS = 1.0e-12;

    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void sameDecodedGenomeAndProfileAlwaysProduceSamePhenotypeAndSnapshot() {
        GenomeAddress health = new GenomeAddress(0x00, Ability.HEALTH.targetId());
        GenomeAddress trait = new GenomeAddress(0x04, Trait.WATCHMAN.targetId());
        DecodedGenome decoded = new DecodedGenome(
                Map.of(
                        health, new AddressAggregate(0.72, 0.80, 0.576, List.of()),
                        trait, new AddressAggregate(0.60, 1.0, 0.60, List.of())),
                List.of());

        WonderfulWolfDecodedPhenotype first = profile.mapPhenotype(decoded);
        WonderfulWolfDecodedPhenotype second = profile.mapPhenotype(decoded);
        assertEquals(first, second);

        DecoderIdentity identity = new DecoderIdentity(1, new byte[32], profile.descriptor());
        assertEquals(
                first.toSnapshot(identity, PhenotypeOrigin.BREEDING),
                second.toSnapshot(identity, PhenotypeOrigin.BREEDING));
    }

    @Test
    void traitThresholdAndStrongGapUseDecoderEpsilonRules() {
        DecodedGenome threshold = new DecodedGenome(
                Map.of(new GenomeAddress(0x04, Trait.WATCHMAN.targetId()),
                        aggregate(0.50 - EPS / 2.0)),
                List.of());
        assertEquals(
                List.of(new ExpressedTrait(Trait.WATCHMAN, TraitStrength.WEAK)),
                profile.mapPhenotype(threshold).expressedTraits());

        DecodedGenome strongBoundary = new DecodedGenome(
                Map.of(
                        new GenomeAddress(0x04, Trait.MINING_SUPPORT.targetId()), aggregate(0.70),
                        new GenomeAddress(0x04, Trait.MUSCLE.targetId()), aggregate(0.575 + EPS / 2.0)),
                List.of());
        assertEquals(
                List.of(new ExpressedTrait(Trait.MINING_SUPPORT, TraitStrength.STRONG)),
                profile.mapPhenotype(strongBoundary).expressedTraits());
    }

    @Test
    void divineThresholdUsesPerHaplotypeAndTotalEpsilonRules() {
        GenomeAddress divine = new GenomeAddress(0x05, 0x00);
        DecodedGenome withinEpsilon = new DecodedGenome(
                Map.of(divine, aggregateOf(List.of(
                        contribution(divine, 0.18 - EPS / 2.0, 0),
                        contribution(divine, 0.47, 1)))),
                List.of());
        assertTrue(profile.mapPhenotype(withinEpsilon).divineLineage().expressed());

        DecodedGenome belowMinimum = new DecodedGenome(
                Map.of(divine, aggregateOf(List.of(
                        contribution(divine, 0.18 - 2.0 * EPS, 0),
                        contribution(divine, 0.48, 1)))),
                List.of());
        assertFalse(profile.mapPhenotype(belowMinimum).divineLineage().expressed());
    }

    @Test
    void personalityDominanceBoundaryUsesDecoderEpsilonRules() {
        DecodedGenome decoded = new DecodedGenome(
                Map.of(
                        new GenomeAddress(0x03, 0x00), centered(0.625 - EPS / 2.0),
                        new GenomeAddress(0x03, 0x01), centered(0.50)),
                List.of());

        assertEquals(Personality.HASTY, profile.mapPhenotype(decoded).personality());
    }

    private static AddressAggregate aggregate(double score) {
        return new AddressAggregate(score, 1.0, score, List.of());
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

    private static EffectiveContribution contribution(
            GenomeAddress address, double saturation, int haplotype) {
        return new EffectiveContribution(address, 1.0, saturation, 0, haplotype, 0, false);
    }

    private static AddressAggregate aggregateOf(List<EffectiveContribution> contributions) {
        double survival = 1.0;
        for (EffectiveContribution contribution : contributions) {
            survival *= 1.0 - contribution.saturation();
        }
        double positive = 1.0 - survival;
        return new AddressAggregate(positive, 1.0, positive, contributions);
    }
}
