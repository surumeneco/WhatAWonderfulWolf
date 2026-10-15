package co.surumene.www.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WwwConfigDefaultsTest {
    @Test
    void loadsDistributedDefaultConfigIntoTypedModel() {
        WwwConfig config = WwwConfigLoader.loadDefaults();

        assertEquals(1, config.configVersion());
        assertEquals(0.42, config.founderTarget().natural().abilities().mean());
        assertEquals(0.22, config.founderTarget().natural().abilities().standardDeviation());
        assertEquals(0.50, config.founderTarget().personality().mean());
        assertEquals(0.10, config.founderTarget().personality().standardDeviation());
        assertEquals(0.500, config.genomeProfile().decoder().personality().seriousMaxScore());
        assertEquals(0.115, config.genomeProfile().decoder().personality().seriousSpread());
        assertEquals(0.085, config.genomeProfile().decoder().personality().dominantGap());
        assertEquals(0.30, config.genomeProfile().synthesizer().personalityCancellationMin());
        assertEquals(0.50, config.genomeProfile().synthesizer().personalityCancellationMax());
        assertEquals(0.50, config.genomeProfile().decoder().trait().expressionThreshold());
        assertEquals(20, config.runtime().combat().retreatMinTicks());
        assertEquals(5.0, config.runtime().actionDistance().narrowBlocks());
        assertEquals(30.0, config.runtime().actionDistance().veryWideBlocks());
        assertEquals("world", config.runtime().age().clockWorld());
        assertEquals(0.05, config.runtime().spawn().naturalConversionProbability());
    }
}
