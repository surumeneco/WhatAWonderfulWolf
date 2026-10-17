package co.surumene.www.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TraitTaskActivityPolicyTest {
    @Test
    void periodicTaskOnlyRunsWithRecurringTraitOrActivePulse() {
        assertFalse(TraitTaskActivityPolicy.shouldRun(0, false));
        assertTrue(TraitTaskActivityPolicy.shouldRun(1, false));
        assertTrue(TraitTaskActivityPolicy.shouldRun(0, true));
        assertTrue(TraitTaskActivityPolicy.shouldRun(50, true));
    }

    @Test
    void onlyZeroSourcesAllowsSchedulerCancellation() {
        assertTrue(TraitTaskActivityPolicy.shouldStop(0, false));
        assertFalse(TraitTaskActivityPolicy.shouldStop(0, true));
        assertFalse(TraitTaskActivityPolicy.shouldStop(1, false));
    }
}
