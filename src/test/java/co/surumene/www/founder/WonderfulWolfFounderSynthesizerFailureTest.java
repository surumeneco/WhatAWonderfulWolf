package co.surumene.www.founder;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.wgl.api.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class WonderfulWolfFounderSynthesizerFailureTest {
    @Test
    void preservesNormalWglSynthesisFailureAsAResult() {
        SynthesisResult.Failure expected = new SynthesisResult.Failure(
                SynthesisFailureReason.CONVERGENCE_LIMIT,
                "diagnostic failure");
        GenomeEngine engine = new FailureEngine(expected);
        WonderfulWolfGenomeProfile profile =
                new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

        FounderGenomeSynthesis synthesis =
                new WonderfulWolfFounderSynthesizer(engine, profile)
                        .synthesize(FounderOrigin.NATURAL, new FixedRandom());

        assertSame(expected, synthesis.result());
        assertEquals(FounderOrigin.NATURAL, synthesis.founderTarget().origin());
    }

    private static final class FailureEngine implements GenomeEngine {
        private final SynthesisResult.Failure failure;

        private FailureEngine(SynthesisResult.Failure failure) {
            this.failure = failure;
        }

        @Override
        public SynthesisResult synthesize(
                GenomeProfile<?> profile,
                BackboneDefinition backbone,
                SynthesisTarget target,
                SynthesisContext context,
                GenomeRandom random) {
            return failure;
        }

        @Override public <P> DecodeResult<P> decode(GenomeProfile<P> profile, DiploidGenome genome) {
            throw new AssertionError("decode must not run after synthesis failure");
        }
        @Override public GenomeRandom standardRandom(long seed) { return new FixedRandom(); }
        @Override public CompatibilityReport assessCompatibility(
                DiploidGenome a, DiploidGenome b, CompatibilityPolicy policy) {
            throw new UnsupportedOperationException();
        }
        @Override public BreedingResult breed(
                GenomeProfile<?> profile, DiploidGenome a, DiploidGenome b,
                BreedingContext context, GenomeRandom random) {
            throw new UnsupportedOperationException();
        }
        @Override public byte[] encode(DiploidGenome genome) { throw new UnsupportedOperationException(); }
        @Override public DiploidGenome decodeBinary(byte[] bytes) { throw new UnsupportedOperationException(); }
        @Override public MarkerResult marker(BackboneDefinition backbone, DiploidGenome genome) {
            throw new UnsupportedOperationException();
        }
        @Override public MarkerResult marker(
                BackboneDefinition backbone, DiploidGenome genome, MarkerScheme scheme) {
            throw new UnsupportedOperationException();
        }
        @Override public GenomeSequenceCodec sequenceCodec() { throw new UnsupportedOperationException(); }
        @Override public GeneSequenceCodec geneSequenceCodec() { throw new UnsupportedOperationException(); }
    }

    private static final class FixedRandom implements GenomeRandom {
        @Override public long nextLong() { return 1L; }
        @Override public double nextDouble() { return 0.5; }
        @Override public int nextInt(int bound) { return 0; }
        @Override public boolean nextBoolean() { return false; }
    }
}
