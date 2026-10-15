package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.domain.RelationshipPerformance;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.DecoderIdentity;
import co.surumene.wgl.api.EffectiveContribution;
import co.surumene.wgl.api.GenomeAddress;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfQuantitativeDecoderTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void emptyDecodedGenomeProducesNeutralNonAbilityPhenotypeAndZeroAbilities() {
        WonderfulWolfDecodedPhenotype phenotype =
                profile.mapPhenotype(new DecodedGenome(Map.of(), List.of()));

        for (Ability ability : Ability.values()) {
            assertEquals(0.0, phenotype.baseAbilities().get(ability));
            assertEquals(0.0, phenotype.extraordinaryContributions().get(ability));
        }
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            assertEquals(0.5, phenotype.personalityFactors().get(factor));
        }
        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            assertEquals(0.5, phenotype.developmentFactors().get(factor));
        }
        assertEquals(Personality.SERIOUS, phenotype.personality());
        assertEquals(new RelationshipPerformance(0, 10), phenotype.relationshipPerformance());
        assertTrue(phenotype.expressedTraits().isEmpty());
        assertTrue(phenotype.injuries().isEmpty());
        assertFalse(phenotype.divineLineage().expressed());
    }

    @Test
    void decodesBaseExtraordinaryRelationshipAndDevelopmentScores() {
        Map<GenomeAddress, AddressAggregate> aggregates = Map.of(
                new GenomeAddress(0x00, Ability.HEALTH.targetId()), aggregate(0.80, 0.75),
                new GenomeAddress(0x07, Ability.HEALTH.targetId()), aggregate(0.60, 0.50),
                new GenomeAddress(0x06, 0x00), aggregate(0.80, 0.80),
                new GenomeAddress(0x06, 0x01), aggregate(0.20, 0.50),
                new GenomeAddress(0x01, DevelopmentFactor.MATURITY.targetId()), aggregate(0.60, 0.75));

        WonderfulWolfDecodedPhenotype phenotype =
                profile.mapPhenotype(new DecodedGenome(aggregates, List.of()));

        assertEquals(0.60, phenotype.baseAbilities().get(Ability.HEALTH), 1.0e-12);
        assertEquals(0.15, phenotype.extraordinaryContributions().get(Ability.HEALTH), 1.0e-12);
        assertEquals(new RelationshipPerformance(60, 9), phenotype.relationshipPerformance());
        assertEquals(0.675, phenotype.developmentFactors().get(DevelopmentFactor.MATURITY), 1.0e-12);
    }

    @Test
    void divineLineageUsesIndependentHaplotypeScoresAndControlsBreedingExtraordinaryExpression() {
        GenomeAddress divine = new GenomeAddress(0x05, 0x00);
        List<EffectiveContribution> contributions = List.of(
                contribution(divine, 0.20, 0, 10),
                contribution(divine, 0.20, 0, 20),
                contribution(divine, 0.20, 1, 10),
                contribution(divine, 0.20, 1, 20));
        AddressAggregate divineAggregate = aggregateOf(contributions);

        Map<GenomeAddress, AddressAggregate> aggregates = Map.of(
                divine, divineAggregate,
                new GenomeAddress(0x00, Ability.HEALTH.targetId()), aggregate(0.90, 1.0),
                new GenomeAddress(0x07, Ability.HEALTH.targetId()), aggregate(0.80, 1.0));

        WonderfulWolfDecodedPhenotype phenotype =
                profile.mapPhenotype(new DecodedGenome(aggregates, List.of()));

        assertEquals(0.36, phenotype.divineLineage().haplotypeAScore(), 1.0e-12);
        assertEquals(0.36, phenotype.divineLineage().haplotypeBScore(), 1.0e-12);
        assertTrue(phenotype.divineLineage().expressed());

        DecoderIdentity identity = new DecoderIdentity(1, new byte[32], profile.descriptor());
        PhenotypeSnapshot breeding = phenotype.toSnapshot(identity, PhenotypeOrigin.BREEDING);
        PhenotypeSnapshot wolfTrap = phenotype.toSnapshot(identity, PhenotypeOrigin.WOLF_TRAP_FOUNDER);

        assertEquals(1.30, breeding.abilities().get(Ability.HEALTH), 1.0e-12);
        assertEquals(0.72, breeding.divineLineageTotalScore(), 1.0e-12);
        assertTrue(breeding.divineLineageExpressed());
        assertEquals(1.30, wolfTrap.abilities().get(Ability.HEALTH), 1.0e-12);
    }

    @Test
    void wolfTrapFounderCanExpressExtraordinaryContributionWithoutDivineLineage() {
        Map<GenomeAddress, AddressAggregate> aggregates = Map.of(
                new GenomeAddress(0x00, Ability.HEALTH.targetId()), aggregate(0.90, 1.0),
                new GenomeAddress(0x07, Ability.HEALTH.targetId()), aggregate(0.80, 1.0));

        WonderfulWolfDecodedPhenotype phenotype =
                profile.mapPhenotype(new DecodedGenome(aggregates, List.of()));
        DecoderIdentity identity = new DecoderIdentity(1, new byte[32], profile.descriptor());

        assertEquals(0.90,
                phenotype.toSnapshot(identity, PhenotypeOrigin.BREEDING).abilities().get(Ability.HEALTH),
                1.0e-12);
        assertEquals(1.30,
                phenotype.toSnapshot(identity, PhenotypeOrigin.WOLF_TRAP_FOUNDER).abilities().get(Ability.HEALTH),
                1.0e-12);
    }

    private static AddressAggregate aggregate(double positive, double negativeSurvival) {
        return new AddressAggregate(positive, negativeSurvival, positive * negativeSurvival, List.of());
    }

    private static EffectiveContribution contribution(
            GenomeAddress address, double saturation, int haplotype, int start) {
        return new EffectiveContribution(address, 1.0, saturation, 0, haplotype, start, false);
    }

    private static AddressAggregate aggregateOf(List<EffectiveContribution> contributions) {
        double positiveSurvival = 1.0;
        double negativeSurvival = 1.0;
        for (EffectiveContribution contribution : contributions) {
            if (contribution.effect() >= 0.0) {
                positiveSurvival *= 1.0 - contribution.saturation();
            } else {
                negativeSurvival *= 1.0 - contribution.saturation();
            }
        }
        double positive = 1.0 - positiveSurvival;
        return new AddressAggregate(
                positive,
                negativeSurvival,
                positive * negativeSurvival,
                contributions);
    }
}
