package co.surumene.www.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AbilityWakeupPolicyTest {
    @Test void idleWaitsForPeriodicAuditRatherThanEachTick() {
        assertEquals(200L, AbilityWakeupPolicy.delay(100L, 700L, false, 300L, 6100L, Long.MAX_VALUE));
    }
    @Test void inactiveJuvenileSourceWaitsForCompatibilityAudit() {
        assertEquals(6000L, AbilityWakeupPolicy.delay(100L, 700L, false,
                Long.MAX_VALUE, 6100L, Long.MAX_VALUE));
    }
    @Test void dirtyOrDeadlineProcessesOnNextTick() {
        assertEquals(1L, AbilityWakeupPolicy.delay(100L, 700L, true, 300L, 6100L, 900L));
        assertEquals(5L, AbilityWakeupPolicy.delay(100L, 700L, false, 300L, 6100L, 705L));
        assertEquals(1L, AbilityWakeupPolicy.delay(100L, 700L, false, 300L, 6100L, 699L));
    }
}
