package co.surumene.www.ui;

import co.surumene.www.domain.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WanWandListenerEligibilityTest {
    @Test
    void everyCommandedModeCanReceiveInstructionsFromAnyPlayer() {
        assertTrue(WanWandListener.canReceiveManualInstruction(Mode.FOLLOW));
        assertTrue(WanWandListener.canReceiveManualInstruction(Mode.GUARD));
        assertTrue(WanWandListener.canReceiveManualInstruction(Mode.WAIT));
    }

    @Test
    void wanderModeDoesNotAcceptManualInstructions() {
        assertFalse(WanWandListener.canReceiveManualInstruction(Mode.WANDER));
    }
}
