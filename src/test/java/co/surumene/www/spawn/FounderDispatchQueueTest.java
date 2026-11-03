package co.surumene.www.spawn;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class FounderDispatchQueueTest {
    @Test
    void submitsOnlyWithinWorkerCapacityAndRetainsRemainingOrder() {
        FounderDispatchQueue<String> queue = new FounderDispatchQueue<>();
        queue.offer("one");
        queue.offer("two");
        queue.offer("three");
        AtomicInteger capacity = new AtomicInteger(1);
        List<String> dispatched = new ArrayList<>();
        assertEquals(1, queue.drain(capacity::get, job -> {
            dispatched.add(job); capacity.decrementAndGet();
        }));
        assertEquals(List.of("one"), dispatched);
        capacity.set(2);
        assertEquals(2, queue.drain(capacity::get, job -> {
            dispatched.add(job); capacity.decrementAndGet();
        }));
        assertEquals(List.of("one", "two", "three"), dispatched);
        assertTrue(queue.isEmpty());
    }

    @Test
    void rejectedSubmissionIsRetriedWithoutLosingHead() {
        FounderDispatchQueue<String> queue = new FounderDispatchQueue<>();
        queue.offer("first");
        queue.offer("second");
        assertEquals(0, queue.drain(() -> 1, job -> {
            throw new java.util.concurrent.RejectedExecutionException();
        }));
        List<String> submitted = new ArrayList<>();
        assertEquals(2, queue.drain(() -> 2, submitted::add));
        assertEquals(List.of("first", "second"), submitted);
    }
}
