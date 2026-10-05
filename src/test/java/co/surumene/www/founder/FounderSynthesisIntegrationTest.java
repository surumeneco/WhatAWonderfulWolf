package co.surumene.www.founder;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.wgl.api.GenomeRandom;
import co.surumene.wgl.api.SynthesisContext;
import co.surumene.wgl.api.SynthesisResult;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class FounderSynthesisIntegrationTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(config, engine.geneSequenceCodec());

    @Test
    void naturalFounderRoundTripsThroughCanonicalDecoderWithinTolerance() {
        SynthesisRun run = synthesize(fixedFounder(FounderOrigin.NATURAL, 0.40), 2026100501L);

        SynthesisResult.Success success = requireSuccess(run.result());
        assertTrue(run.target().isSatisfied(success.decoded().decodedGenome(), 0.002));
    }

    @Test
    void extraordinaryWolfTrapFounderRoundTripsThroughCanonicalDecoderWithinTolerance() {
        SynthesisRun run = synthesize(fixedFounder(FounderOrigin.WOLF_TRAP, 1.20), 2026100502L);

        SynthesisResult.Success success = requireSuccess(run.result());
        assertTrue(run.target().isSatisfied(success.decoded().decodedGenome(), 0.002));
    }

    @Test
    void sameSeedReproducesTheWholeFounderGenerationPipeline() {
        WonderfulWolfFounderSynthesizer synthesizer =
                new WonderfulWolfFounderSynthesizer(engine, profile);

        FounderGenomeSynthesis first =
                synthesizer.synthesize(FounderOrigin.NATURAL, 2026100503L);
        FounderGenomeSynthesis second =
                synthesizer.synthesize(FounderOrigin.NATURAL, 2026100503L);

        assertEquals(first.founderTarget(), second.founderTarget());
        assertEquals(first.synthesisTarget().continuousTargets(),
                second.synthesisTarget().continuousTargets());

        SynthesisResult.Success a = requireSuccess(first.result());
        SynthesisResult.Success b = requireSuccess(second.result());
        assertArrayEquals(engine.encode(a.genome()), engine.encode(b.genome()));
        assertTrue(first.synthesisTarget().isSatisfied(
                a.decoded().decodedGenome(),
                EngineConfig.defaults().synthesizer().convergenceTolerance()));
    }

    private static SynthesisResult.Success requireSuccess(SynthesisResult result) {
        if (result instanceof SynthesisResult.Success success) {
            return success;
        }
        SynthesisResult.Failure failure = (SynthesisResult.Failure) result;
        fail("synthesis failed: " + failure.reason() + " / " + failure.detail());
        throw new AssertionError("unreachable");
    }

    private SynthesisRun synthesize(FounderTarget founder, long seed) {
        GenomeRandom random = engine.standardRandom(seed);
        WonderfulWolfSynthesisTarget target =
                WonderfulWolfSynthesisTarget.from(founder, config, random);
        SynthesisResult result = engine.synthesize(
                profile,
                profile.backbone(),
                target,
                SynthesisContext.defaults(),
                random);
        if (result instanceof SynthesisResult.Success success) {
            result = new SynthesisResult.Success(
                    success.genome(),
                    engine.decode(profile, success.genome()));
        }
        return new SynthesisRun(target, result);
    }

    private static FounderTarget fixedFounder(FounderOrigin origin, double abilityValue) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, abilityValue);

        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) personality.put(factor, 0.5);

        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) development.put(factor, 0.5);

        return new FounderTarget(
                origin,
                abilities,
                personality,
                List.of(),
                development,
                new FounderRelationshipTarget(0.5, 0.5));
    }

    private record SynthesisRun(
            WonderfulWolfSynthesisTarget target,
            SynthesisResult result) {}
}
