package co.surumene.www.spawn;

import org.junit.jupiter.api.Test;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class FounderWorkQueueTest {
    @Test void drainsOnlyWhenSpaceIsAvailableAndKeepsRejectedWork() {
        FounderWorkQueue<Integer> queue = new FounderWorkQueue<>();
        queue.offer(1);
        queue.offer(2);
        AtomicInteger capacity = new AtomicInteger(1);
        assertEquals(1, queue.drain(capacity::get, value -> {
            assertEquals(1, value);
            capacity.decrementAndGet();
        }));
        assertEquals(1, queue.size());
        assertEquals(0, queue.drain(() -> 0, value -> fail()));
        assertEquals(0, queue.drain(() -> 1, value -> {
            throw new RejectedExecutionException();
        }));
        assertEquals(1, queue.size());
        assertEquals(1, queue.drain(() -> 1, value -> assertEquals(2, value)));
        assertTrue(queue.isEmpty());
    }
}
