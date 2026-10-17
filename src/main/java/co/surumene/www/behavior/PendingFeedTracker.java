package co.surumene.www.behavior;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongConsumer;

public final class PendingFeedTracker {
    private final Map<UUID, PendingFeed> pending = new HashMap<>();
    private LongConsumer wakeup = unused -> {};
    private long armedDue = Long.MAX_VALUE;

    public void onExpiryWakeup(LongConsumer callback) {
        wakeup = java.util.Objects.requireNonNull(callback);
        armNext();
    }

    public int pendingCount() { return pending.size(); }

    public void expireAt(long tick) { cleanup(tick); }

    public void record(
            UUID wolfId,
            UUID playerId,
            long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must be >= 0");
        }
        pending.put(
                java.util.Objects.requireNonNull(wolfId, "wolfId"),
                new PendingFeed(
                        java.util.Objects.requireNonNull(playerId, "playerId"),
                        serverTick));
        armNext();
    }

    public Optional<UUID> consume(
            UUID wolfId,
            long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must be >= 0");
        }
        PendingFeed feed = pending.remove(
                java.util.Objects.requireNonNull(wolfId, "wolfId"));
        if (feed == null) {
            return Optional.empty();
        }
        long age = serverTick - feed.serverTick();
        return age >= 0L && age <= 1L
                ? Optional.of(feed.playerId())
                : Optional.empty();
    }

    public void cleanup(long serverTick) {
        Iterator<Map.Entry<UUID, PendingFeed>> iterator =
                pending.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingFeed feed = iterator.next().getValue();
            if (serverTick - feed.serverTick() > 1L) {
                iterator.remove();
            }
        }
        if (serverTick >= armedDue) armedDue = Long.MAX_VALUE;
        armNext();
    }

    private void armNext() {
        long earliest = Long.MAX_VALUE;
        for (PendingFeed feed : pending.values()) {
            long due = feed.serverTick() >= Long.MAX_VALUE - 2L
                    ? Long.MAX_VALUE : feed.serverTick() + 2L;
            if (due < earliest) earliest = due;
        }
        if (earliest < armedDue) {
            armedDue = earliest;
            wakeup.accept(earliest);
        }
    }

    private record PendingFeed(UUID playerId, long serverTick) {}
}
