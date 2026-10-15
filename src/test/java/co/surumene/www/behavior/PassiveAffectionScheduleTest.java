package co.surumene.www.behavior;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class PassiveAffectionScheduleTest {
    private static final UUID WOLF =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID A =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID B =
            UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void firstObservationStartsTheIntervalAndOnlyBecomesDueAtTheBoundary() {
        PassiveAffectionSchedule schedule = new PassiveAffectionSchedule();

        assertFalse(schedule.isDue(WOLF, A, 100L, 1200L));
        assertFalse(schedule.isDue(WOLF, A, 1299L, 1200L));
        assertTrue(schedule.isDue(WOLF, A, 1300L, 1200L));
        assertFalse(schedule.isDue(WOLF, A, 1301L, 1200L));
    }

    @Test
    void changingCommanderRestartsTheFullIntervalForTheNewCommander() {
        PassiveAffectionSchedule schedule = new PassiveAffectionSchedule();

        assertFalse(schedule.isDue(WOLF, A, 0L, 1200L));
        assertFalse(schedule.isDue(WOLF, B, 1199L, 1200L));
        assertFalse(schedule.isDue(WOLF, B, 1200L, 1200L));
        assertTrue(schedule.isDue(WOLF, B, 2399L, 1200L));
    }

    @Test
    void retainAndClearDropUnloadedOrInactiveRuntimeState() {
        PassiveAffectionSchedule schedule = new PassiveAffectionSchedule();
        UUID other = UUID.randomUUID();

        schedule.isDue(WOLF, A, 0L, 1200L);
        schedule.isDue(other, A, 0L, 1200L);
        schedule.retain(Set.of(WOLF));
        schedule.clear(WOLF);

        assertFalse(schedule.isDue(WOLF, A, 5000L, 1200L));
        assertFalse(schedule.isDue(other, A, 5000L, 1200L));
    }
}
