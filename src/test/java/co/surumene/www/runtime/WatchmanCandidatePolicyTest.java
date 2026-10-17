package co.surumene.www.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WatchmanCandidatePolicyTest {
    @Test void excludesOutOfRangeAndNonLivingEntitiesBeforeExpensiveEngagementLookup() {
        assertFalse(WatchmanCandidatePolicy.shouldInspect(false, false, false, true, 0, 25));
        assertFalse(WatchmanCandidatePolicy.shouldInspect(true, true, false, true, 0, 25));
        assertFalse(WatchmanCandidatePolicy.shouldInspect(true, false, true, true, 0, 25));
        assertFalse(WatchmanCandidatePolicy.shouldInspect(true, false, false, false, 0, 25));
        assertFalse(WatchmanCandidatePolicy.shouldInspect(true, false, false, true, 26, 25));
        assertTrue(WatchmanCandidatePolicy.shouldInspect(true, false, false, true, 25, 25));
    }
}
