package co.surumene.www.behavior;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class AffectionCompetitionResolverTest {
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID C = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    void aSingleValidInstructionIsAcceptedWithoutAWeightedContest() {
        assertEquals(
                A,
                AffectionCompetitionResolver.choose(Map.of(A, -999L), 0.999));
    }

    @Test
    void negativeMinimumIsShiftedAndTheMinimumCandidateGetsZeroWeight() {
        LinkedHashMap<UUID, Long> candidates = new LinkedHashMap<>();
        candidates.put(A, -10L);
        candidates.put(B, 10L);

        assertEquals(B, AffectionCompetitionResolver.choose(candidates, 0.0));
        assertEquals(B, AffectionCompetitionResolver.choose(candidates, 0.999));
    }

    @Test
    void nonNegativeValuesUseTheirAbsoluteAffectionAsWeights() {
        LinkedHashMap<UUID, Long> candidates = new LinkedHashMap<>();
        candidates.put(A, 10L);
        candidates.put(B, 20L);

        assertEquals(A, AffectionCompetitionResolver.choose(candidates, 0.0));
        assertEquals(A, AffectionCompetitionResolver.choose(candidates, 0.32));
        assertEquals(B, AffectionCompetitionResolver.choose(candidates, 0.34));
    }

    @Test
    void allZeroWeightsFallBackToEqualProbability() {
        LinkedHashMap<UUID, Long> candidates = new LinkedHashMap<>();
        candidates.put(A, -5L);
        candidates.put(B, -5L);
        candidates.put(C, -5L);

        assertEquals(A, AffectionCompetitionResolver.choose(candidates, 0.0));
        assertEquals(B, AffectionCompetitionResolver.choose(candidates, 0.34));
        assertEquals(C, AffectionCompetitionResolver.choose(candidates, 0.99));
    }

    @Test
    void threeCandidatesWithANegativeMinimumExcludeOnlyTheMinimum() {
        LinkedHashMap<UUID, Long> candidates = new LinkedHashMap<>();
        candidates.put(A, -10L);
        candidates.put(B, 0L);
        candidates.put(C, 10L);

        for (double draw : new double[]{0.0, 0.1, 0.4, 0.9, 0.999}) {
            assertNotEquals(A, AffectionCompetitionResolver.choose(candidates, draw));
        }
    }
}
