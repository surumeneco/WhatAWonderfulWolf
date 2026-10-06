package co.surumene.www.behavior;

import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.entity.Wolf;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WonderfulWolfRelationshipService {
    private final WonderfulWolfLoadedIndividuals loaded;

    public WonderfulWolfRelationshipService(
            WonderfulWolfLoadedIndividuals loaded) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
    }

    public Optional<Long> current(Wolf wolf, UUID playerId) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(playerId, "playerId");
        return loaded.find(wolf.getUniqueId())
                .map(individual -> AffectionPolicy.current(individual, playerId));
    }

    public boolean reward(Wolf wolf, UUID playerId) {
        return update(wolf, playerId, true);
    }

    public boolean penalize(Wolf wolf, UUID playerId) {
        return update(wolf, playerId, false);
    }

    private boolean update(
            Wolf wolf,
            UUID playerId,
            boolean reward) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(playerId, "playerId");

        WonderfulWolfIndividual current =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (current == null) {
            return false;
        }

        WonderfulWolfIndividual updated = reward
                ? AffectionPolicy.reward(current, playerId)
                : AffectionPolicy.penalize(current, playerId);
        loaded.saveAndRegister(wolf, updated);
        return true;
    }
}
