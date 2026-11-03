package co.surumene.www.behavior;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongConsumer;

public final class PendingFeedTracker {
    private final Map<UUID, PendingFeed> pending = new HashMap<>();
    private LongConsumer scheduleCleanup;
    private long scheduledFor = Long.MIN_VALUE;

    /** Installs a single earliest-deadline wakeup; supplied delay is in server ticks. */
    public void onCleanupDue(LongConsumer scheduleCleanup) {
        this.scheduleCleanup = java.util.Objects.requireNonNull(scheduleCleanup);
    }

    private void schedule(long now, long due) {
        if (scheduleCleanup == null || (scheduledFor != Long.MIN_VALUE && scheduledFor <= due)) return;
        scheduledFor = due;
        scheduleCleanup.accept(Math.max(1L, due - now));
    }

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
        schedule(serverTick, serverTick + 2L);
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
        scheduledFor = Long.MIN_VALUE;
        long nextDue = Long.MAX_VALUE;
        Iterator<Map.Entry<UUID, PendingFeed>> iterator =
                pending.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingFeed feed = iterator.next().getValue();
            if (serverTick - feed.serverTick() > 1L) {
                iterator.remove();
            } else {
                nextDue = Math.min(nextDue, feed.serverTick() + 2L);
            }
        }
        if (nextDue != Long.MAX_VALUE) schedule(serverTick, nextDue);
    }

    private record PendingFeed(UUID playerId, long serverTick) {}
}
