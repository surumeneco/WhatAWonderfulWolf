package co.surumene.www.breeding;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.*;
import co.surumene.www.founder.*;
import co.surumene.www.genome.*;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class WonderfulWolfBreedingContextIntegrationTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(config, engine.geneSequenceCodec());
    private final WonderfulWolfBreedingContextFactory factory =
            new WonderfulWolfBreedingContextFactory(engine, profile);
    private WonderfulWolfIndividual directFounder;
    private WonderfulWolfIndividual plainFounder;
    private WonderfulWolfIndividual extraordinaryFounder;

    @BeforeAll
    void setUpParents() {
        directFounder = founder(
                FounderOrigin.NATURAL,
                0.45,
                List.of(new ExpressedTrait(
                        Trait.DIRECT_INHERITANCE,
                        TraitStrength.WEAK)),
                2026100611L);
        plainFounder = founder(
                FounderOrigin.NATURAL,
                0.40,
                List.of(),
                2026100612L);
        extraordinaryFounder = founder(
                FounderOrigin.WOLF_TRAP,
                1.20,
                List.of(),
                2026100614L);
    }

    @Test
    void directInheritanceCreatesOnlySoftPhysicalConstraints() {
        WonderfulWolfIndividual direct = directFounder;
        WonderfulWolfIndividual plain = plainFounder;

        BreedingContext context =
                factory.create(direct, plain, engine.standardRandom(2026100613L));

        assertEquals(Set.of(new GenomeAddress(0x05, 0x00)), context.deNovoForbiddenAddresses());
        assertTrue(context.parentAPolicy().inheritanceConstraints().stream().anyMatch(c ->
                !c.hardProtection()
                        && c.retentionProbability() == 0.75
                        && c.crossoverWeightMultiplier() == 0.25));
        assertTrue(context.parentAPolicy().inheritanceConstraints().stream().noneMatch(
                InheritanceConstraint::hardProtection));
        assertTrue(context.parentBPolicy().inheritanceConstraints().isEmpty());
    }

    @Test
    void typedParentSourcesRebuildModifiersOnlyForDiploidParents() {
        BreedingParentSource diploid =
                new BreedingParentSource.DiploidParent(
                        directFounder.genome());
        BreedingParentSource gamete =
                new BreedingParentSource.Gamete(
                        haplotypeA(plainFounder.genome()));

        BreedingContext context =
                factory.create(
                        diploid,
                        gamete,
                        engine.standardRandom(2026100707L));

        assertEquals(1.0, context.mutationRateMultiplier());
        assertTrue(context.parentAPolicy().inheritanceConstraints().stream()
                .anyMatch(c -> !c.hardProtection()));
        assertTrue(context.parentBPolicy().inheritanceConstraints().isEmpty());
    }

    @Test
    void typedParentGameteDoesNotReapplyDirectInheritance() {
        BreedingContext context = factory.create(
                new BreedingParentSource.Gamete(
                        haplotypeA(directFounder.genome())),
                new BreedingParentSource.Gamete(
                        haplotypeA(plainFounder.genome())),
                engine.standardRandom(2026100709L));

        assertEquals(1.0, context.mutationRateMultiplier());
        assertTrue(context.parentAPolicy().inheritanceConstraints().isEmpty());
        assertTrue(context.parentBPolicy().inheritanceConstraints().isEmpty());
    }

    @Test
    void typedParentDiploidRebuildsWildMutationButGameteDoesNot() {
        WonderfulWolfIndividual wild = founder(
                FounderOrigin.NATURAL,
                0.45,
                List.of(new ExpressedTrait(Trait.WILD, TraitStrength.STRONG)),
                2026100710L);

        BreedingParentSource diploid =
                new BreedingParentSource.DiploidParent(wild.genome());
        BreedingParentSource gamete =
                new BreedingParentSource.Gamete(
                        haplotypeA(wild.genome()));
        BreedingParentSource plainGamete =
                new BreedingParentSource.Gamete(
                        haplotypeA(plainFounder.genome()));

        BreedingContext diploidContext = factory.create(
                diploid,
                plainGamete,
                engine.standardRandom(2026100711L));
        BreedingContext gameteContext = factory.create(
                gamete,
                plainGamete,
                engine.standardRandom(2026100712L));

        assertEquals(Optional.of(TraitStrength.STRONG),
                WonderfulWolfBreedingPolicy.expressedTrait(
                        wild.phenotypeSnapshot(), Trait.WILD));
        assertEquals(4.0, diploidContext.mutationRateMultiplier());
        assertEquals(1.0, gameteContext.mutationRateMultiplier());
        assertTrue(gameteContext.parentAPolicy().inheritanceConstraints().isEmpty());
        assertTrue(gameteContext.parentBPolicy().inheritanceConstraints().isEmpty());
    }

    @Test
    void extraordinaryAbilityWithoutExpressedDivineLineageDoesNotCreateHardProtection() {
        WonderfulWolfIndividual extraordinary =
                withDivineState(extraordinaryFounder, false);
        WonderfulWolfIndividual plain = plainFounder;

        BreedingContext context =
                factory.create(extraordinary, plain, engine.standardRandom(2026100619L));

        assertTrue(context.parentAPolicy().inheritanceConstraints().stream().noneMatch(
                InheritanceConstraint::hardProtection));
        assertTrue(context.parentBPolicy().inheritanceConstraints().isEmpty());
    }

    @Test
    void expressedDivineLineageCreatesHardExtraordinaryBlockProtection() {
        WonderfulWolfIndividual extraordinary =
                withDivineExpressed(extraordinaryFounder);
        WonderfulWolfIndividual plain = plainFounder;

        BreedingContext context =
                factory.create(extraordinary, plain, engine.standardRandom(2026100616L));

        assertTrue(context.parentAPolicy().inheritanceConstraints().stream().anyMatch(c ->
                c.hardProtection()
                        && c.retentionProbability() == 1.0
                        && c.crossoverWeightMultiplier() == 0.0));
        assertTrue(context.parentBPolicy().inheritanceConstraints().isEmpty());
    }

    private WonderfulWolfIndividual withDivineExpressed(WonderfulWolfIndividual source) {
        PhenotypeSnapshot p = source.phenotypeSnapshot();
        PhenotypeSnapshot divine = new PhenotypeSnapshot(
                p.decoderIdentity(),
                p.abilities(),
                p.relationshipPerformance(),
                p.personalityFactors(),
                p.personality(),
                p.expressedTraits(),
                p.developmentFactors(),
                p.injuries(),
                Math.max(0.65, p.divineLineageTotalScore()),
                true);
        return copyWithSnapshot(source, divine);
    }

    private WonderfulWolfIndividual withDivineState(
            WonderfulWolfIndividual source,
            boolean expressed) {
        PhenotypeSnapshot p = source.phenotypeSnapshot();
        PhenotypeSnapshot adjusted = new PhenotypeSnapshot(
                p.decoderIdentity(),
                p.abilities(),
                p.relationshipPerformance(),
                p.personalityFactors(),
                p.personality(),
                p.expressedTraits(),
                p.developmentFactors(),
                p.injuries(),
                p.divineLineageTotalScore(),
                expressed);
        return copyWithSnapshot(source, adjusted);
    }

    private WonderfulWolfIndividual founder(
            FounderOrigin origin,
            double ability,
            List<ExpressedTrait> traits,
            long seed) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability value : Ability.values()) abilities.put(value, ability);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor value : PersonalityFactor.values()) personality.put(value, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor value : DevelopmentFactor.values()) development.put(value, 0.5);

        FounderTarget target = new FounderTarget(
                origin,
                abilities,
                personality,
                traits,
                development,
                new FounderRelationshipTarget(0.5, 0.5));
        GenomeRandom random = engine.standardRandom(seed);
        WonderfulWolfSynthesisTarget synthesisTarget =
                WonderfulWolfSynthesisTarget.from(target, config, random);
        SynthesisResult.Success synthesized = assertInstanceOf(
                SynthesisResult.Success.class,
                engine.synthesize(
                        profile,
                        profile.backbone(),
                        synthesisTarget,
                        SynthesisContext.defaults(),
                        random));
        DecodeResult<WonderfulWolfDecodedPhenotype> canonical =
                engine.decode(profile, synthesized.genome());
        PhenotypeSnapshot snapshot = canonical.phenotype().toSnapshot(
                canonical.identity(),
                origin == FounderOrigin.WOLF_TRAP
                        ? PhenotypeOrigin.WOLF_TRAP_FOUNDER
                        : PhenotypeOrigin.NATURAL_FOUNDER);
        return new WonderfulWolfIndividual(
                synthesized.genome(),
                snapshot,
                Optional.empty(),
                0,
                Mode.WANDER,
                Optional.empty(),
                ActionDistance.NORMAL,
                Optional.empty(),
                Map.of(),
                Optional.empty(),
                Map.of(),
                0,
                PedigreeSnapshot.founder());
    }

    private static HaploidGenome haplotypeA(
            DiploidGenome genome) {
        return new HaploidGenome(
                genome.genomeFormatVersion(),
                genome.chromosomePairs().stream()
                        .map(ChromosomePair::haplotypeA)
                        .toList());
    }

    private static WonderfulWolfIndividual copyWithSnapshot(
            WonderfulWolfIndividual source,
            PhenotypeSnapshot snapshot) {
        return new WonderfulWolfIndividual(
                source.genome(),
                snapshot,
                source.ownerId(),
                source.adultBiologicalTime(),
                source.mode(),
                source.commanderId(),
                source.actionDistance(),
                source.waitLocation(),
                source.affection(),
                source.weapon(),
                source.inventory(),
                source.generation(),
                source.pedigree());
    }
}
