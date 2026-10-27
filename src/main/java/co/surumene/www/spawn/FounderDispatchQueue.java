package co.surumene.www.spawn;

import co.surumene.wcl.api.runtime.BoundedDispatchQueue;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Consumer-local facade keeping the original API while WCL owns FIFO admission.
 * The wolf plugin remains responsible for worker capacity and completion wakeups.
 */
final class FounderDispatchQueue<T> {
    private final BoundedDispatchQueue<T> waiting = new BoundedDispatchQueue<>();

    void offer(T job) {
        waiting.offer(job);
    }

    int size() {
        return waiting.size();
    }

    boolean isEmpty() {
        return waiting.isEmpty();
    }

    void clear() {
        waiting.clear();
    }

    int drain(IntSupplier capacity, Consumer<T> submit) {
        return waiting.drain(capacity, submit);
    }
}
