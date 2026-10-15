package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.wgl.api.AnchorSeed;
import co.surumene.wgl.api.BitSequence;
import co.surumene.wgl.api.ChromosomeTemplate;
import co.surumene.wgl.api.GenomeRandom;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfFounderMarkerVariationTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void markerAnchorsIndependentlyChooseFromCanonicalPlusFortyEightSingleBitVariants() {
        ChromosomeTemplate template = profile.backbone().chromosomes().getFirst();
        AnchorSeed first = template.markerLocus().first();
        AnchorSeed second = template.markerLocus().second();

        BitSequence haplotypeA = profile.founderTemplateBits(
                0, 0, template, new ChoiceRandom(1, 0));
        BitSequence haplotypeB = profile.founderTemplateBits(
                0, 1, template, new ChoiceRandom(48, 13));

        assertEquals(1, markerWindow(haplotypeA, first).hammingDistance(first.canonicalBits()));
        assertEquals(0, markerWindow(haplotypeA, second).hammingDistance(second.canonicalBits()));
        assertEquals(1, markerWindow(haplotypeB, first).hammingDistance(first.canonicalBits()));
        assertEquals(1, markerWindow(haplotypeB, second).hammingDistance(second.canonicalBits()));

        assertTrue(markerWindow(haplotypeA, first).hammingDistance(
                markerWindow(haplotypeB, first)) <= 2);
        assertTrue(markerWindow(haplotypeA, second).hammingDistance(
                markerWindow(haplotypeB, second)) <= 2);

        List<AnchorSeed> nonMarker = template.anchors().stream()
                .filter(anchor -> !anchor.equals(first) && !anchor.equals(second))
                .toList();
        for (AnchorSeed anchor : nonMarker) {
            assertEquals(
                    anchor.canonicalBits(),
                    markerWindow(haplotypeA, anchor));
            assertEquals(
                    anchor.canonicalBits(),
                    markerWindow(haplotypeB, anchor));
        }
    }

    @Test
    void zeroChoiceKeepsBothMarkerAnchorsCanonical() {
        ChromosomeTemplate template = profile.backbone().chromosomes().getFirst();
        BitSequence varied = profile.founderTemplateBits(
                0, 0, template, new ChoiceRandom(0, 0));

        assertEquals(template.templateBits(), varied);
    }

    private static BitSequence markerWindow(BitSequence bits, AnchorSeed anchor) {
        return bits.slice(anchor.position(), anchor.position() + 48);
    }

    private static final class ChoiceRandom implements GenomeRandom {
        private final Queue<Integer> choices = new ArrayDeque<>();

        private ChoiceRandom(int... values) {
            for (int value : values) choices.add(value);
        }

        @Override public long nextLong() { return 0L; }
        @Override public double nextDouble() { return 0.5; }
        @Override public boolean nextBoolean() { return false; }

        @Override
        public int nextInt(int bound) {
            assertEquals(49, bound);
            return choices.remove();
        }
    }
}
