package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.GenomeAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfTraitDecoderTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void belowThresholdProducesNoTrait() {
        var decoded = decoded(Map.of(0x00, 0.499));
        assertTrue(profile.mapPhenotype(decoded).expressedTraits().isEmpty());
    }

    @Test
    void singleCandidateIsWeakEvenWhenFarAboveOthers() {
        var decoded = decoded(Map.of(0x06, 0.90));
        assertEquals(List.of(new ExpressedTrait(Trait.WATCHMAN, TraitStrength.WEAK)),
                profile.mapPhenotype(decoded).expressedTraits());
    }

    @Test
    void strongGapBoundaryProducesOnlyStrongTopTrait() {
        var decoded = decoded(Map.of(0x00, 0.700, 0x01, 0.575));
        assertEquals(List.of(new ExpressedTrait(Trait.MINING_SUPPORT, TraitStrength.STRONG)),
                profile.mapPhenotype(decoded).expressedTraits());
    }

    @Test
    void closeScoresProduceTwoWeakTraitsInScoreThenTargetOrder() {
        var decoded = decoded(Map.of(0x02, 0.60, 0x01, 0.60, 0x00, 0.55));
        assertEquals(List.of(
                        new ExpressedTrait(Trait.MUSCLE, TraitStrength.WEAK),
                        new ExpressedTrait(Trait.GUARDIAN, TraitStrength.WEAK)),
                profile.mapPhenotype(decoded).expressedTraits());
    }

    private static DecodedGenome decoded(Map<Integer, Double> scores) {
        java.util.HashMap<GenomeAddress, AddressAggregate> aggregates = new java.util.HashMap<>();
        scores.forEach((target, score) -> aggregates.put(
                new GenomeAddress(0x04, target),
                new AddressAggregate(score, 1.0, score, List.of())));
        return new DecodedGenome(aggregates, List.of());
    }
}
