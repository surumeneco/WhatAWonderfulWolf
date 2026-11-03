package co.surumene.www.behavior;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class PendingFeedWakeupTest {
    @Test
    void multiplePendingFeedsUseOneEarliestDeadlineAndNeverChangeTheOneTickWindow() {
        PendingFeedTracker tracker = new PendingFeedTracker();
        List<Long> delays = new ArrayList<>();
        tracker.onCleanupDue(delays::add);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), player = UUID.randomUUID();
        tracker.record(first, player, 100L);
        tracker.record(second, player, 101L);
        assertEquals(List.of(2L), delays);
        assertTrue(tracker.consume(first, 102L).isEmpty());
        tracker.cleanup(102L);
        assertEquals(List.of(2L, 1L), delays);
        assertEquals(player, tracker.consume(second, 102L).orElseThrow());
        tracker.cleanup(103L);
        assertEquals(List.of(2L, 1L), delays);
    }

    @Test
    void repeatedFeedsAndEmptyRecordsDoNotAccumulateTasks() {
        PendingFeedTracker tracker = new PendingFeedTracker();
        List<Long> delays = new ArrayList<>();
        tracker.onCleanupDue(delays::add);
        UUID wolf = UUID.randomUUID(), player = UUID.randomUUID();
        tracker.record(wolf, player, 20L);
        tracker.record(wolf, player, 21L);
        assertEquals(List.of(2L), delays);
        tracker.cleanup(22L);
        assertEquals(List.of(2L, 1L), delays);
        tracker.cleanup(23L);
        assertTrue(tracker.consume(wolf, 23L).isEmpty());
    }
}
