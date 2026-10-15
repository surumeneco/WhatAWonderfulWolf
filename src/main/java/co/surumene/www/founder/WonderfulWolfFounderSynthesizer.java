package co.surumene.www.founder;

import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.wgl.api.DecodeResult;
import co.surumene.wgl.api.GenomeEngine;
import co.surumene.wgl.api.GenomeRandom;
import co.surumene.wgl.api.SynthesisContext;
import co.surumene.wgl.api.SynthesisResult;

import java.util.Objects;

public final class WonderfulWolfFounderSynthesizer {
    private final GenomeEngine engine;
    private final WonderfulWolfGenomeProfile profile;
    private final WonderfulWolfFounderTargetGenerator targetGenerator;

    public WonderfulWolfFounderSynthesizer(
            GenomeEngine engine,
            WonderfulWolfGenomeProfile profile) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.targetGenerator = new WonderfulWolfFounderTargetGenerator(profile.config());
    }

    public FounderGenomeSynthesis synthesize(FounderOrigin origin, long seed) {
        return synthesize(origin, engine.standardRandom(seed));
    }

    public FounderGenomeSynthesis synthesize(
            FounderOrigin origin,
            GenomeRandom random) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(random, "random");

        FounderTarget founderTarget = targetGenerator.generate(origin, random);
        WonderfulWolfSynthesisTarget synthesisTarget =
                WonderfulWolfSynthesisTarget.from(
                        founderTarget,
                        profile.config(),
                        random);

        SynthesisResult result = engine.synthesize(
                profile,
                profile.backbone(),
                synthesisTarget,
                SynthesisContext.defaults(),
                random);

        if (result instanceof SynthesisResult.Success success) {
            DecodeResult<?> canonical = engine.decode(profile, success.genome());
            result = new SynthesisResult.Success(success.genome(), canonical);
        }

        return new FounderGenomeSynthesis(
                founderTarget,
                synthesisTarget,
                result);
    }
}
