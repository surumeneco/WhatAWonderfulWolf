package co.surumene.www.behavior;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class PendingFeedTrackerTest {
    @Test
    void onlySameOrNextTickHealingConsumesARecordedFeeder() {
        PendingFeedTracker tracker = new PendingFeedTracker();
        UUID wolf = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        tracker.record(wolf, player, 100L);
        assertEquals(
                player,
                tracker.consume(wolf, 101L).orElseThrow());

        tracker.record(wolf, player, 200L);
        assertTrue(tracker.consume(wolf, 202L).isEmpty());
    }

    @Test
    void stalePendingFeedsAreCleanedUp() {
        PendingFeedTracker tracker = new PendingFeedTracker();
        UUID wolf = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        tracker.record(wolf, player, 100L);
        tracker.cleanup(102L);
        assertTrue(tracker.consume(wolf, 102L).isEmpty());
    }
}
