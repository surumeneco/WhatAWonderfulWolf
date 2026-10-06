package co.surumene.www.behavior;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PendingFeedTracker {
    private final Map<UUID, PendingFeed> pending = new HashMap<>();

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
    }

    private record PendingFeed(UUID playerId, long serverTick) {}
}
