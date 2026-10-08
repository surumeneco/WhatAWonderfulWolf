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
final class WonderfulWolfBreedingServiceIntegrationTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(config, engine.geneSequenceCodec());
    private final WonderfulWolfBreedingService service =
            new WonderfulWolfBreedingService(engine, () -> profile);
    private WonderfulWolfIndividual alpha;
    private WonderfulWolfIndividual beta;

    @BeforeAll
    void setUpParents() {
        alpha = founder(0.35, 2026100601L);
        beta = founder(0.45, 2026100602L);
    }

    @Test
    void breedsF1FromParentGenomesAndBuildsStaticSnapshotAndPedigree() {
        WonderfulWolfIndividual a = alpha;
        WonderfulWolfIndividual b = beta;

        WonderfulWolfBreedingOutcome.Success success = assertInstanceOf(
                WonderfulWolfBreedingOutcome.Success.class,
                service.breed(
                        new BreedingParent("Alpha", a),
                        new BreedingParent("Beta", b),
                        Optional.empty(),
                        2026100603L));

        WonderfulWolfIndividual child = success.child();
        assertEquals(1, child.generation());
        assertEquals("Alpha", child.pedigree().parentA().orElseThrow().ancestor().displayName());
        assertEquals("Beta", child.pedigree().parentB().orElseThrow().ancestor().displayName());
        assertEquals(
                engine.marker(profile.backbone(), a.genome()).formatted(),
                child.pedigree().parentA().orElseThrow().ancestor().lineageId());
        assertEquals(
                engine.marker(profile.backbone(), b.genome()).formatted(),
                child.pedigree().parentB().orElseThrow().ancestor().lineageId());
        assertTrue(child.pedigree().grandparentAA().isEmpty());
        assertTrue(child.ownerId().isEmpty());
        assertEquals(Mode.WANDER, child.mode());
        assertTrue(child.commanderId().isEmpty());
        assertTrue(child.affection().isEmpty());

        DecodeResult<WonderfulWolfDecodedPhenotype> canonical =
                engine.decode(profile, child.genome());
        assertEquals(
                canonical.phenotype().toSnapshot(
                        canonical.identity(),
                        PhenotypeOrigin.BREEDING),
                child.phenotypeSnapshot());
        assertEquals(
                engine.marker(profile.backbone(), child.genome()).formatted(),
                success.lineageId());
    }

    @Test
    void batchBreedingProducesValidChildrenWithoutFallback() {
        for (int i = 0; i < 8; i++) {
            WonderfulWolfBreedingOutcome.Success success = assertInstanceOf(
                    WonderfulWolfBreedingOutcome.Success.class,
                    service.breed(
                            new BreedingParent("Alpha", alpha),
                            new BreedingParent("Beta", beta),
                            Optional.empty(),
                            202610070000L + i));

            assertEquals(1, success.child().generation());
            assertEquals(6, success.child().genome().chromosomePairs().size());
            assertFalse(success.lineageId().isBlank());
        }
    }

    @Test
    void incompatibleParentsFallBackWithoutSynthesizingAReplacementChild() {
        WonderfulWolfIndividual valid = alpha;
        DiploidGenome incompatibleGenome = new DiploidGenome(
                valid.genome().genomeFormatVersion(),
                List.of(valid.genome().chromosomePairs().getFirst()));
        WonderfulWolfIndividual incompatible = new WonderfulWolfIndividual(
                incompatibleGenome,
                valid.phenotypeSnapshot(),
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

        assertInstanceOf(
                WonderfulWolfBreedingOutcome.Fallback.class,
                service.breed(
                        new BreedingParent("Valid", valid),
                        new BreedingParent("Incompatible", incompatible),
                        Optional.empty(),
                        2026100605L));
    }

    @Test
    void mutuallyCompatibleParentsStillFallBackWhenTheyDoNotMatchWolfBackbone() {
        List<ChromosomePair> pairs = new ArrayList<>();
        for (var chromosome : profile.backbone().chromosomes()) {
            String bits = chromosome.templateBits().toBitString();
            StringBuilder inverted = new StringBuilder(bits.length());
            for (int i = 0; i < bits.length(); i++) {
                inverted.append(bits.charAt(i) == '0' ? '1' : '0');
            }
            BitSequence sequence = BitSequence.fromBits(inverted.toString());
            pairs.add(new ChromosomePair(sequence, sequence));
        }
        DiploidGenome offBackbone = new DiploidGenome(1, pairs);
        WonderfulWolfIndividual template = alpha;
        WonderfulWolfIndividual a = new WonderfulWolfIndividual(
                offBackbone,
                template.phenotypeSnapshot(),
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
        WonderfulWolfIndividual b = copy(
                a, a.phenotypeSnapshot(), 0, PedigreeSnapshot.founder());

        assertTrue(engine.assessCompatibility(
                offBackbone, offBackbone, null).compatible());
        assertFalse(engine.assessBackboneCompatibility(
                profile.backbone(), offBackbone).compatible());

        assertInstanceOf(
                WonderfulWolfBreedingOutcome.Fallback.class,
                service.breed(
                        new BreedingParent("A", a),
                        new BreedingParent("B", b),
                        Optional.empty(),
                        2026100702L));
    }

    @Test
    void childGenomeComesFromParentGenomesNotEditedParentAbilitySnapshots() {
        WonderfulWolfIndividual a = alpha;
        WonderfulWolfIndividual b = beta;

        WonderfulWolfBreedingOutcome.Success baseline = assertInstanceOf(
                WonderfulWolfBreedingOutcome.Success.class,
                service.breed(
                        new BreedingParent("Alpha", a),
                        new BreedingParent("Beta", b),
                        Optional.empty(),
                        2026100623L));

        WonderfulWolfIndividual editedA = withAbilities(a, 1.0);
        WonderfulWolfIndividual editedB = withAbilities(b, 0.0);
        WonderfulWolfBreedingOutcome.Success edited = assertInstanceOf(
                WonderfulWolfBreedingOutcome.Success.class,
                service.breed(
                        new BreedingParent("Alpha", editedA),
                        new BreedingParent("Beta", editedB),
                        Optional.empty(),
                        2026100623L));

        assertArrayEquals(
                engine.encode(baseline.child().genome()),
                engine.encode(edited.child().genome()));
    }

    @Test
    void copiesParentPedigreesIntoTheFourGrandparentSlots() {
        WonderfulWolfIndividual a = withPedigree(
                alpha,
                "A-A",
                "A-B");
        WonderfulWolfIndividual b = withPedigree(
                beta,
                "B-A",
                "B-B");

        WonderfulWolfBreedingOutcome.Success success = assertInstanceOf(
                WonderfulWolfBreedingOutcome.Success.class,
                service.breed(
                        new BreedingParent("Alpha", a),
                        new BreedingParent("Beta", b),
                        Optional.empty(),
                        2026100626L));

        PedigreeSnapshot pedigree = success.child().pedigree();
        assertEquals(2, success.child().generation());
        assertEquals(
                "A-A",
                pedigree.grandparentAA().orElseThrow().displayName());
        assertEquals(
                "A-B",
                pedigree.grandparentAB().orElseThrow().displayName());
        assertEquals(
                "B-A",
                pedigree.grandparentBA().orElseThrow().displayName());
        assertEquals(
                "B-B",
                pedigree.grandparentBB().orElseThrow().displayName());
    }

    private WonderfulWolfIndividual founder(double ability, long seed) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability value : Ability.values()) {
            abilities.put(value, ability);
        }
        EnumMap<PersonalityFactor, Double> personality =
                new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor value : PersonalityFactor.values()) {
            personality.put(value, 0.5);
        }
        EnumMap<DevelopmentFactor, Double> development =
                new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor value : DevelopmentFactor.values()) {
            development.put(value, 0.5);
        }

        FounderTarget target = new FounderTarget(
                FounderOrigin.NATURAL,
                abilities,
                personality,
                List.of(),
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

        return new WonderfulWolfIndividual(
                synthesized.genome(),
                canonical.phenotype().toSnapshot(
                        canonical.identity(),
                        PhenotypeOrigin.NATURAL_FOUNDER),
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

    private static WonderfulWolfIndividual withAbilities(
            WonderfulWolfIndividual source,
            double value) {
        PhenotypeSnapshot p = source.phenotypeSnapshot();
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            abilities.put(ability, value);
        }
        PhenotypeSnapshot edited = new PhenotypeSnapshot(
                p.decoderIdentity(),
                abilities,
                p.relationshipPerformance(),
                p.personalityFactors(),
                p.personality(),
                p.expressedTraits(),
                p.developmentFactors(),
                p.injuries(),
                p.divineLineageTotalScore(),
                false);
        return copy(
                source,
                edited,
                source.generation(),
                source.pedigree());
    }

    private static WonderfulWolfIndividual withPedigree(
            WonderfulWolfIndividual source,
            String parentAName,
            String parentBName) {
        ParentSnapshot parentA = new ParentSnapshot(
                new AncestorSnapshot(
                        parentAName,
                        0,
                        "01-02-03-04-05-06"),
                Personality.SERIOUS.name(),
                List.of(),
                false);
        ParentSnapshot parentB = new ParentSnapshot(
                new AncestorSnapshot(
                        parentBName,
                        0,
                        "11-12-13-14-15-16"),
                Personality.SERIOUS.name(),
                List.of(),
                false);
        return copy(
                source,
                source.phenotypeSnapshot(),
                1,
                new PedigreeSnapshot(
                        Optional.of(parentA),
                        Optional.of(parentB),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));
    }

    private static WonderfulWolfIndividual copy(
            WonderfulWolfIndividual source,
            PhenotypeSnapshot snapshot,
            int generation,
            PedigreeSnapshot pedigree) {
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
                generation,
                pedigree);
    }
}
