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
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfBreedingServiceIntegrationTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(config, engine.geneSequenceCodec());
    private final WonderfulWolfBreedingService service =
            new WonderfulWolfBreedingService(engine, () -> profile);

    @Test
    void breedsF1FromParentGenomesAndBuildsStaticSnapshotAndPedigree() {
        WonderfulWolfIndividual a = founder(0.35, 2026100601L);
        WonderfulWolfIndividual b = founder(0.45, 2026100602L);

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
        assertEquals(engine.marker(profile.backbone(), a.genome()).formatted(),
                child.pedigree().parentA().orElseThrow().ancestor().lineageId());
        assertEquals(engine.marker(profile.backbone(), b.genome()).formatted(),
                child.pedigree().parentB().orElseThrow().ancestor().lineageId());
        assertTrue(child.pedigree().grandparentAA().isEmpty());
        assertTrue(child.ownerId().isEmpty());
        assertEquals(Mode.WANDER, child.mode());
        assertTrue(child.commanderId().isEmpty());
        assertTrue(child.affection().isEmpty());

        DecodeResult<WonderfulWolfDecodedPhenotype> canonical =
                engine.decode(profile, child.genome());
        assertEquals(
                canonical.phenotype().toSnapshot(canonical.identity(), PhenotypeOrigin.BREEDING),
                child.phenotypeSnapshot());
        assertEquals(engine.marker(profile.backbone(), child.genome()).formatted(),
                success.lineageId());
    }

    @Test
    void incompatibleParentsFallBackWithoutSynthesizingAReplacementChild() {
        WonderfulWolfIndividual valid = founder(0.40, 2026100604L);
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

    private WonderfulWolfIndividual founder(double ability, long seed) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability value : Ability.values()) abilities.put(value, ability);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor value : PersonalityFactor.values()) personality.put(value, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor value : DevelopmentFactor.values()) development.put(value, 0.5);

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
                        canonical.identity(), PhenotypeOrigin.NATURAL_FOUNDER),
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
}
