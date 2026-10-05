package co.surumene.www.persistence;

import co.surumene.www.individual.WonderfulWolfIndividual;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WonderfulWolfLoadedIndividuals {
    private final WonderfulWolfEntityStore store;
    private final Map<UUID, LoadedIndividual> loaded = new HashMap<>();

    public WonderfulWolfLoadedIndividuals(WonderfulWolfEntityStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public RestoreResult register(Entity entity) {
        RestoreResult result = store.restore(entity);
        if (!(entity instanceof Wolf wolf)) {
            return result;
        }

        UUID id = wolf.getUniqueId();
        if (result instanceof RestoreResult.Success success) {
            loaded.put(id, new LoadedIndividual(wolf, success.individual()));
        } else {
            loaded.remove(id);
        }
        return result;
    }

    public void saveAndRegister(Wolf wolf, WonderfulWolfIndividual individual) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(individual, "individual");
        store.save(wolf, individual);
        loaded.put(wolf.getUniqueId(), new LoadedIndividual(wolf, individual));
    }

    public void unregister(Entity entity) {
        if (!(entity instanceof Wolf wolf)) {
            return;
        }

        LoadedIndividual current = loaded.get(wolf.getUniqueId());
        if (current != null && current.entity() == wolf) {
            loaded.remove(wolf.getUniqueId());
        }
    }

    public Optional<WonderfulWolfIndividual> find(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId");
        LoadedIndividual current = loaded.get(entityId);
        return current == null ? Optional.empty() : Optional.of(current.individual());
    }

    public int size() {
        return loaded.size();
    }

    public void clear() {
        loaded.clear();
    }

    private record LoadedIndividual(Wolf entity, WonderfulWolfIndividual individual) {
        private LoadedIndividual {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(individual, "individual");
        }
    }
}
