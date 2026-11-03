package co.surumene.www.behavior;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class PassiveAffectionDeadlineQueueTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void emptyQueueDoesNotProduceWork() {
        PassiveAffectionDeadlineQueue queue = new PassiveAffectionDeadlineQueue();
        assertTrue(queue.takeReady(0L).isEmpty());
        assertTrue(queue.takeReady(100000L).isEmpty());
    }

    @Test
    void changedWolfIsConsideredExactlyOnceAndThenOnlyAtItsDeadline() {
        PassiveAffectionDeadlineQueue queue = new PassiveAffectionDeadlineQueue();
        queue.changed(A);
        assertEquals(Set.of(A), queue.takeReady(5L));
        assertTrue(queue.takeReady(10L).isEmpty());

        queue.schedule(A, 1205L);
        assertTrue(queue.takeReady(1204L).isEmpty());
        assertEquals(Set.of(A), queue.takeReady(1205L));
        assertTrue(queue.takeReady(1210L).isEmpty());
    }

    @Test
    void updateReplacesStaleDeadlineWithoutDuplicatingWork() {
        PassiveAffectionDeadlineQueue queue = new PassiveAffectionDeadlineQueue();
        queue.schedule(A, 1200L);
        queue.changed(A);
        assertEquals(Set.of(A), queue.takeReady(10L));
        assertTrue(queue.takeReady(1200L).isEmpty());
        queue.schedule(A, 2400L);
        queue.schedule(A, 3000L);
        assertTrue(queue.takeReady(2400L).isEmpty());
        assertEquals(Set.of(A), queue.takeReady(3000L));
    }

    @Test
    void discardingUnloadedWolfCancelsAllPendingWork() {
        PassiveAffectionDeadlineQueue queue = new PassiveAffectionDeadlineQueue();
        queue.changed(A);
        queue.schedule(A, 100L);
        queue.discard(A);
        assertTrue(queue.takeReady(100L).isEmpty());
        queue.changed(A);
        assertEquals(Set.of(A), queue.takeReady(105L));
    }

    @Test
    void dueQueueTouchesOnlyReadyMembersEvenWithManyRegistered() {
        PassiveAffectionDeadlineQueue queue = new PassiveAffectionDeadlineQueue();
        for (int i = 0; i < 1000; ++i) {
            queue.schedule(new UUID(0L, i + 100L), 1200L);
        }
        queue.schedule(B, 100L);
        assertEquals(Set.of(B), queue.takeReady(100L));
        assertTrue(queue.takeReady(200L).isEmpty());
        assertEquals(1000, queue.takeReady(1200L).size());
    }

    @Test
    void rejectsNegativeTicksAndNullIds() {
        PassiveAffectionDeadlineQueue queue = new PassiveAffectionDeadlineQueue();
        assertThrows(IllegalArgumentException.class, () -> queue.takeReady(-1L));
        assertThrows(IllegalArgumentException.class, () -> queue.schedule(A, -1L));
        assertThrows(NullPointerException.class, () -> queue.changed(null));
    }
}
