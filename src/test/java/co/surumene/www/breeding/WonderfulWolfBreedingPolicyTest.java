package co.surumene.www.breeding;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.*;
import co.surumene.wgl.api.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfBreedingPolicyTest {
    private final WwwConfig.BreedingPolicy policy =
            WwwConfigLoader.loadDefaults().genomeProfile().breedingPolicy();

    @Test
    void wildTraitMultipliersAreRecomputedFromBothParentSnapshots() {
        PhenotypeSnapshot weak = snapshot(List.of(new ExpressedTrait(Trait.WILD, TraitStrength.WEAK)));
        PhenotypeSnapshot strong = snapshot(List.of(new ExpressedTrait(Trait.WILD, TraitStrength.STRONG)));
        PhenotypeSnapshot none = snapshot(List.of());

        assertEquals(8.0, WonderfulWolfBreedingPolicy.mutationMultiplier(weak, strong, policy));
        assertEquals(4.0, WonderfulWolfBreedingPolicy.mutationMultiplier(weak, weak, policy));
        assertEquals(16.0, WonderfulWolfBreedingPolicy.mutationMultiplier(strong, strong, policy));
        assertEquals(1.0, WonderfulWolfBreedingPolicy.mutationMultiplier(none, none, policy));
    }

    @Test
    void directInheritanceStrengthControlsRetentionButNotCrossoverWeight() {
        assertEquals(0.75,
                WonderfulWolfBreedingPolicy.directRetentionProbability(TraitStrength.WEAK, policy));
        assertEquals(1.0,
                WonderfulWolfBreedingPolicy.directRetentionProbability(TraitStrength.STRONG, policy));
        assertEquals(0.25, policy.directInheritance().crossoverWeightInsideBlock());
    }

    @Test
    void divineAddressIsAlwaysForbiddenFromDeNovoCreation() {
        assertEquals(Set.of(new GenomeAddress(0x05, 0x00)),
                WonderfulWolfBreedingPolicy.deNovoForbiddenAddresses());
    }

    @Test
    void hardBlocksSuppressOverlappingSoftBlocksEvenAcrossHaplotypes() {
        InheritanceConstraint hard = InheritanceConstraint.hard(0, 0, 100, 200);
        InheritanceConstraint overlappingSoft =
                InheritanceConstraint.soft(0, 1, 150, 250, 0.75, 0.25);
        InheritanceConstraint adjacentSoft =
                InheritanceConstraint.soft(0, 1, 200, 260, 0.75, 0.25);
        InheritanceConstraint otherChromosome =
                InheritanceConstraint.soft(1, 0, 120, 180, 0.75, 0.25);

        assertEquals(
                List.of(adjacentSoft, otherChromosome),
                WonderfulWolfBreedingPolicy.resolveSoftAgainstHard(
                        List.of(hard),
                        List.of(overlappingSoft, adjacentSoft, otherChromosome)));
    }

    private static PhenotypeSnapshot snapshot(List<ExpressedTrait> traits) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 0.5);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) personality.put(factor, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) development.put(factor, 0.5);

        return new PhenotypeSnapshot(
                new DecoderIdentity(1, new byte[32],
                        new ProfileDescriptor("wonderful-wolf", 1, new byte[32])),
                abilities,
                new RelationshipPerformance(0, 10),
                personality,
                Personality.SERIOUS,
                traits,
                development,
                List.of(),
                0.0,
                false);
    }
}
