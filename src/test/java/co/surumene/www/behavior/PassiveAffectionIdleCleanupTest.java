package co.surumene.www.behavior;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PassiveAffectionIdleCleanupTest {
    @Test void absenceOfEligibleWolvesClearsOldSchedule() {
        var schedule = new PassiveAffectionSchedule();
        var wolf=UUID.randomUUID();
        var commander=UUID.randomUUID();
        assertFalse(schedule.isDue(wolf,commander,0,10));
        assertTrue(schedule.isDue(wolf,commander,10,10));
        schedule.retain(Set.of());
        assertFalse(schedule.isDue(wolf,commander,20,10));
    }
}
