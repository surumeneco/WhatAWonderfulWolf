package co.surumene.www.spawn;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Bounded-worker submissions; invoked only on enqueue or completion, on the server thread. */
final class FounderDispatchQueue<T> {
    private final ArrayDeque<T> waiting = new ArrayDeque<>();

    void offer(T work) { waiting.addLast(Objects.requireNonNull(work)); }
    int size() { return waiting.size(); }
    boolean isEmpty() { return waiting.isEmpty(); }
    void clear() { waiting.clear(); }

    int drain(IntSupplier capacity, Consumer<T> submit) {
        int count = 0;
        while (!waiting.isEmpty() && capacity.getAsInt() > 0) {
            T job = waiting.removeFirst();
            try {
                submit.accept(job);
                count++;
            } catch (RejectedExecutionException rejected) {
                waiting.addFirst(job);
                break;
            }
        }
        return count;
    }
}
