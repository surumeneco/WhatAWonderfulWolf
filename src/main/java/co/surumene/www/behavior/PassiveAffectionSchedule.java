package co.surumene.www.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PassiveAffectionSchedule {
    private final Map<UUID, State> states = new HashMap<>();

    public boolean isDue(
            UUID wolfId,
            UUID commanderId,
            long serverTick,
            long intervalTicks) {
        Objects.requireNonNull(wolfId, "wolfId");
        Objects.requireNonNull(commanderId, "commanderId");
        if (serverTick < 0L || intervalTicks < 0L) {
            throw new IllegalArgumentException("ticks must be >= 0");
        }

        State state = states.get(wolfId);
        if (state == null || !state.commanderId().equals(commanderId)) {
            states.put(wolfId, new State(commanderId, serverTick));
            return false;
        }

        long elapsed = Math.max(0L, serverTick - state.lastCheckTick());
        if (elapsed < intervalTicks) {
            return false;
        }

        states.put(wolfId, new State(commanderId, serverTick));
        return true;
    }

    public void clear(UUID wolfId) {
        states.remove(Objects.requireNonNull(wolfId, "wolfId"));
    }

    public void retain(Set<UUID> loadedWolfIds) {
        Objects.requireNonNull(loadedWolfIds, "loadedWolfIds");
        states.keySet().retainAll(loadedWolfIds);
    }

    private record State(UUID commanderId, long lastCheckTick) {
        private State {
            Objects.requireNonNull(commanderId, "commanderId");
            if (lastCheckTick < 0L) {
                throw new IllegalArgumentException(
                        "lastCheckTick must be >= 0");
            }
        }
    }
}
