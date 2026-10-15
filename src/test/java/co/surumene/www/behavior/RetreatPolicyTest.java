package co.surumene.www.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RetreatPolicyTest {
    @Test
    void patienceMapsToOneMinusPatienceThreshold() {
        assertEquals(0.0, RetreatPolicy.threshold(1.0), 1.0e-12);
        assertEquals(0.25, RetreatPolicy.threshold(0.75), 1.0e-12);
        assertEquals(1.0, RetreatPolicy.threshold(0.0), 1.0e-12);
    }

    @Test
    void retreatStartsAtOrBelowThresholdAndHonorsMinimumTicks() {
        RetreatState state = RetreatState.inactive();

        state = RetreatPolicy.evaluate(state, 0.25, 0.75, 100L, 20L);
        assertTrue(state.active());
        assertEquals(100L, state.startedTick());

        assertTrue(RetreatPolicy.evaluate(
                state, 0.90, 0.75, 119L, 20L).active());
        assertFalse(RetreatPolicy.evaluate(
                state, 0.90, 0.75, 120L, 20L).active());
    }

    @Test
    void retreatContinuesAfterMinimumTimeWhileHealthStillMeetsCondition() {
        RetreatState state = new RetreatState(true, 100L);
        assertTrue(RetreatPolicy.evaluate(
                state, 0.20, 0.75, 500L, 20L).active());
    }
}
