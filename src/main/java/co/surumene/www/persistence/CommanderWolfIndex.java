package co.surumene.www.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Main-thread incremental commander index; constant-time idle lookups. */
public final class CommanderWolfIndex {
    private final Map<UUID, UUID> ownerByWolf = new HashMap<>();
    private final Map<UUID, Set<UUID>> wolvesByOwner = new HashMap<>();

    public void update(UUID wolf, UUID owner) {
        Objects.requireNonNull(wolf, "wolf");
        UUID old = ownerByWolf.get(wolf);
        if (Objects.equals(old, owner)) return;
        if (old != null) {
            Set<UUID> previous = wolvesByOwner.get(old);
            if (previous != null) {
                previous.remove(wolf);
                if (previous.isEmpty()) wolvesByOwner.remove(old);
            }
        }
        if (owner == null) {
            ownerByWolf.remove(wolf);
            return;
        }
        ownerByWolf.put(wolf, owner);
        wolvesByOwner.computeIfAbsent(owner, ignored -> new HashSet<>()).add(wolf);
    }

    public void remove(UUID wolf) {
        update(wolf, null);
    }

    public Set<UUID> ids(UUID owner) {
        if (owner == null) return Set.of();
        Set<UUID> wolves = wolvesByOwner.get(owner);
        return wolves == null || wolves.isEmpty() ? Set.of() : Set.copyOf(wolves);
    }

    public void clear() {
        ownerByWolf.clear();
        wolvesByOwner.clear();
    }
}
