package co.surumene.www.founder;

import co.surumene.wgl.api.SynthesisResult;

import java.util.Objects;

public record FounderGenomeSynthesis(
        FounderTarget founderTarget,
        WonderfulWolfSynthesisTarget synthesisTarget,
        SynthesisResult result) {

    public FounderGenomeSynthesis {
        Objects.requireNonNull(founderTarget, "founderTarget");
        Objects.requireNonNull(synthesisTarget, "synthesisTarget");
        Objects.requireNonNull(result, "result");
    }
}
