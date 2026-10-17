package co.surumene.www.spawn;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

final class FounderWorkQueue<T> {
  private final ArrayDeque<T> pending = new ArrayDeque<>();
  void offer(T task) { pending.addLast(Objects.requireNonNull(task)); }
  int size() { return pending.size(); }
  boolean isEmpty() { return pending.isEmpty(); }
  void clear() { pending.clear(); }
  int drain(IntSupplier capacity, Consumer<T> submit) {
    int dispatched = 0;
    while (!pending.isEmpty() && capacity.getAsInt() > 0) {
      T task = pending.removeFirst();
      try { submit.accept(task); dispatched++; }
      catch (RejectedExecutionException rejected) { pending.addFirst(task); break; }
    }
    return dispatched;
  }
}
