package co.surumene.www.behavior;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Main-thread deadline index for passive affection. Work is indexed by UUID,
 * never by a Bukkit Entity, so unload/replacement can cancel stale entries.
 * The caller drives this with the existing five-tick scheduler.
 */
public final class PassiveAffectionDeadlineQueue {
    private final Set<UUID> changed = new LinkedHashSet<>();
    private final Map<UUID, Long> dueForWolf = new HashMap<>();
    private final NavigableMap<Long, Set<UUID>> wolvesDueAt = new TreeMap<>();

    public void changed(UUID wolfId) {
        Objects.requireNonNull(wolfId, "wolfId");
        unschedule(wolfId);
        changed.add(wolfId);
    }

    public void schedule(UUID wolfId, long serverTick) {
        Objects.requireNonNull(wolfId, "wolfId");
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must be >= 0");
        }
        changed.remove(wolfId);
        unschedule(wolfId);
        dueForWolf.put(wolfId, serverTick);
        wolvesDueAt.computeIfAbsent(serverTick, ignored -> new HashSet<>())
                .add(wolfId);
    }

    public void discard(UUID wolfId) {
        Objects.requireNonNull(wolfId, "wolfId");
        changed.remove(wolfId);
        unschedule(wolfId);
    }

    /** Drain only changed/expired IDs; return no candidates when nothing is due. */
    public Set<UUID> takeReady(long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must be >= 0");
        }
        if (changed.isEmpty()
                && (wolvesDueAt.isEmpty() || wolvesDueAt.firstKey() > serverTick)) {
            return Set.of();
        }
        Set<UUID> ready = new LinkedHashSet<>(changed);
        changed.clear();
        while (!wolvesDueAt.isEmpty() && wolvesDueAt.firstKey() <= serverTick) {
            Map.Entry<Long, Set<UUID>> due = wolvesDueAt.pollFirstEntry();
            for (UUID id : due.getValue()) {
                if (dueForWolf.remove(id, due.getKey())) {
                    ready.add(id);
                }
            }
        }
        return ready;
    }

    private void unschedule(UUID wolfId) {
        Long tick = dueForWolf.remove(wolfId);
        if (tick == null) {
            return;
        }
        Set<UUID> scheduled = wolvesDueAt.get(tick);
        if (scheduled != null) {
            scheduled.remove(wolfId);
            if (scheduled.isEmpty()) {
                wolvesDueAt.remove(tick);
            }
        }
    }
}
