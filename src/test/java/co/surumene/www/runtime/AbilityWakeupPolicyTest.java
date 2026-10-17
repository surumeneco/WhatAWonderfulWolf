package co.surumene.www.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AbilityWakeupPolicyTest {
    @Test void dormantRuntimeHasNoScheduledWork() {
        assertEquals(Long.MAX_VALUE,
                AbilityWakeupPolicy.delay(200L, false, false, false, 0L, 6000L, Long.MAX_VALUE, 1000L));
    }
    @Test void dirtyWorkWakesOnTheNextTick() {
        assertEquals(1L,
                AbilityWakeupPolicy.delay(200L, true, false, false, 0L, 6000L, Long.MAX_VALUE, 1000L));
    }
    @Test void adultAgeTimeDueBeforeAuditWakesBeforeHeartbeat() {
        assertEquals(25L,
                AbilityWakeupPolicy.delay(200L, false, true, false, 400L, 6000L, 1025L, 1000L));
    }
    @Test void juvenileAndCompatibilityCheckKeepTheirDeadlines() {
        assertEquals(40L,
                AbilityWakeupPolicy.delay(200L, false, true, true, 240L, 6000L, Long.MAX_VALUE, 1000L));
        assertEquals(1L,
                AbilityWakeupPolicy.delay(6000L, false, true, false, 6100L, 6000L, Long.MAX_VALUE, 1000L));
    }
    @Test void checkpointForBiologicalClockRollbackIsBounded() {
        assertEquals(200L,
                AbilityWakeupPolicy.delay(0L, false, true, false, 200L, 6000L, 100000L, 1L));
    }
}
