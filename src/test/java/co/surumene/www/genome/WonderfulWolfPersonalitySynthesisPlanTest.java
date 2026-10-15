package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeRandom;
import co.surumene.wgl.api.SynthesisAddressPlan;
import co.surumene.wgl.api.SynthesisContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfPersonalitySynthesisPlanTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void neutralFounderPersonalityKeepsBothPositiveAndNegativeContributions() {
        SynthesisAddressPlan plan = profile.synthesisPlan(
                new GenomeAddress(0x03, 0x00),
                0.5,
                SynthesisContext.defaults(),
                new FixedRandom(0.5));

        assertEquals(plan.positiveSaturation(), plan.negativeSaturation(), 1.0e-12);
        assertTrue(plan.positiveSaturation() >= 0.30);
        assertTrue(plan.positiveSaturation() <= 0.50);
        assertTrue(plan.minPositiveGenes() > 0);
        assertTrue(plan.minNegativeGenes() > 0);
    }

    @Test
    void nonNeutralFounderPersonalityKeepsHiddenOppositeSignContribution() {
        SynthesisAddressPlan plan = profile.synthesisPlan(
                new GenomeAddress(0x03, 0x00),
                0.70,
                SynthesisContext.defaults(),
                new FixedRandom(0.5));

        assertEquals(0.40, plan.positiveSaturation() - plan.negativeSaturation(), 1.0e-12);
        assertTrue(plan.negativeSaturation() > 0.0);
        assertTrue(plan.minPositiveGenes() > 0);
        assertTrue(plan.minNegativeGenes() > 0);
    }

    private record FixedRandom(double value) implements GenomeRandom {
        @Override public long nextLong() { return 0L; }
        @Override public double nextDouble() { return value; }
        @Override public int nextInt(int bound) { return 0; }
        @Override public boolean nextBoolean() { return false; }
    }
}
