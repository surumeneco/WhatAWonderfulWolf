package co.surumene.www.behavior;

import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ManualTargetRegistry {
    private final Map<UUID, ManualTarget> targets = new java.util.HashMap<>();

    public boolean submit(
            WonderfulWolfIndividual individual,
            UUID wolfId,
            UUID issuerId,
            UUID targetId,
            double draw) {
        Objects.requireNonNull(individual, "individual");
        Objects.requireNonNull(wolfId, "wolfId");
        Objects.requireNonNull(issuerId, "issuerId");
        Objects.requireNonNull(targetId, "targetId");

        ManualTarget current = targets.get(wolfId);
        if (current == null || current.issuerId().equals(issuerId)) {
            targets.put(wolfId, new ManualTarget(issuerId, targetId));
            return true;
        }

        LinkedHashMap<UUID, Long> weights = new LinkedHashMap<>();
        weights.put(
                current.issuerId(),
                AffectionPolicy.current(individual, current.issuerId()));
        weights.put(
                issuerId,
                AffectionPolicy.current(individual, issuerId));
        UUID winner = AffectionCompetitionResolver.choose(weights, draw);
        if (!winner.equals(issuerId)) {
            return false;
        }

        targets.put(wolfId, new ManualTarget(issuerId, targetId));
        return true;
    }

    public Optional<ManualTarget> get(UUID wolfId) {
        return Optional.ofNullable(
                targets.get(Objects.requireNonNull(wolfId, "wolfId")));
    }

    public void clear(UUID wolfId) {
        targets.remove(Objects.requireNonNull(wolfId, "wolfId"));
    }

    public void clearAll() {
        targets.clear();
    }

    public record ManualTarget(UUID issuerId, UUID targetId) {
        public ManualTarget {
            Objects.requireNonNull(issuerId, "issuerId");
            Objects.requireNonNull(targetId, "targetId");
        }
    }
}
