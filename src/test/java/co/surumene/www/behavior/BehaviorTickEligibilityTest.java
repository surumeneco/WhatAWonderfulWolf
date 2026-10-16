package co.surumene.www.behavior;

import co.surumene.www.domain.Mode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BehaviorTickEligibilityTest {
    @Test void nonWanderingModesNeedPeriodicEvaluation() {
        for (Mode mode : Mode.values()) {
            assertEquals(mode != Mode.WANDER, BehaviorTickEligibility.periodic(mode));
        }
    }

    @Test void wanderingWolvesBecomeActiveForReactiveCombatOrRetreat() {
        assertFalse(BehaviorTickEligibility.evaluate(Mode.WANDER, false));
        assertTrue(BehaviorTickEligibility.evaluate(Mode.WANDER, true));
        assertTrue(BehaviorTickEligibility.evaluate(Mode.FOLLOW, false));
    }
}
