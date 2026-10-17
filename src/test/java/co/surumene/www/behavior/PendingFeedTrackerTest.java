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
    @Test
    void schedulesOneEarliestExpiryAndRetainsNewerFeedUntilItsDeadline() {
        java.util.ArrayList<Long> wakeups = new java.util.ArrayList<>();
        PendingFeedTracker tracker = new PendingFeedTracker();
        tracker.onExpiryWakeup(wakeups::add);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        tracker.record(first, player, 100L);
        tracker.record(second, player, 101L);
        assertEquals(java.util.List.of(102L), wakeups);
        tracker.expireAt(102L);
        assertEquals(java.util.List.of(102L, 103L), wakeups);
        assertTrue(tracker.consume(first, 102L).isEmpty());
        assertEquals(player, tracker.consume(second, 102L).orElseThrow());
        tracker.expireAt(103L);
        assertEquals(0, tracker.pendingCount());
    }
}
