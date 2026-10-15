package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Personality;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.GenomeAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WonderfulWolfPersonalityDecoderTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void neutralVectorIsSerious() {
        assertEquals(Personality.SERIOUS,
                profile.mapPhenotype(new DecodedGenome(Map.of(), List.of())).personality());
    }

    @Test
    void dominantFactorUsesSameFactorPair() {
        var decoded = new DecodedGenome(Map.of(
                new GenomeAddress(0x03, 0x00), centered(0.80),
                new GenomeAddress(0x03, 0x01), centered(0.60)), List.of());

        assertEquals(Personality.HASTY, profile.mapPhenotype(decoded).personality());
    }

    @Test
    void nonDominantTopTwoUsePairTable() {
        var decoded = new DecodedGenome(Map.of(
                new GenomeAddress(0x03, 0x02), centered(0.75),
                new GenomeAddress(0x03, 0x03), centered(0.70)), List.of());

        assertEquals(Personality.MIGHTY, profile.mapPhenotype(decoded).personality());
    }

    @Test
    void allFactorsBelowSeriousMaximumAreSerious() {
        var decoded = new DecodedGenome(Map.of(
                new GenomeAddress(0x03, 0x00), centered(0.49),
                new GenomeAddress(0x03, 0x01), centered(0.40),
                new GenomeAddress(0x03, 0x02), centered(0.35),
                new GenomeAddress(0x03, 0x03), centered(0.30),
                new GenomeAddress(0x03, 0x04), centered(0.25),
                new GenomeAddress(0x03, 0x05), centered(0.20)), List.of());

        assertEquals(Personality.SERIOUS, profile.mapPhenotype(decoded).personality());
    }

    @Test
    void equallyStrongFactorsAreSeriousWhenSpreadIsSmall() {
        var decoded = new DecodedGenome(Map.of(
                new GenomeAddress(0x03, 0x00), centered(0.75),
                new GenomeAddress(0x03, 0x01), centered(0.75),
                new GenomeAddress(0x03, 0x02), centered(0.75),
                new GenomeAddress(0x03, 0x03), centered(0.75),
                new GenomeAddress(0x03, 0x04), centered(0.75),
                new GenomeAddress(0x03, 0x05), centered(0.75)), List.of());

        assertEquals(Personality.SERIOUS, profile.mapPhenotype(decoded).personality());
    }

    @Test
    void exactTopTieUsesLowerTargetIdsWhenVectorIsNotSerious() {
        var decoded = new DecodedGenome(Map.of(
                new GenomeAddress(0x03, 0x00), centered(0.70),
                new GenomeAddress(0x03, 0x01), centered(0.70),
                new GenomeAddress(0x03, 0x02), centered(0.40)), List.of());

        assertEquals(Personality.JOLLY, profile.mapPhenotype(decoded).personality());
    }

    @Test
    void dominantGapUsesAbsolutePointZeroEightFiveThreshold() {
        var decoded = new DecodedGenome(Map.of(
                new GenomeAddress(0x03, 0x00), centered(0.700),
                new GenomeAddress(0x03, 0x01), centered(0.615),
                new GenomeAddress(0x03, 0x02), centered(0.40)), List.of());

        assertEquals(Personality.HASTY, profile.mapPhenotype(decoded).personality());
    }

    private static AddressAggregate centered(double score) {
        double positive = Math.max(0.0, 2.0 * score - 1.0);
        double negativeSurvival = score >= 0.5 ? 1.0 : 2.0 * score;
        return new AddressAggregate(
                positive,
                negativeSurvival,
                positive * negativeSurvival,
                List.of());
    }
}
