package co.surumene.www.persistence;

import co.surumene.www.individual.WonderfulWolfIndividual;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class WonderfulWolfLoadedIndividuals {
    private final WonderfulWolfEntityStore store;
    private final Map<UUID, LoadedIndividual> loaded = new HashMap<>();
    private final CommanderWolfIndex commanders = new CommanderWolfIndex();
    private Consumer<Wolf> firstCreation = wolf -> {};
    private Consumer<Wolf> weaponSync = wolf -> {};
    private Consumer<UUID> removal = id -> {};

    public void onWeaponSync(Consumer<Wolf> callback) {
        weaponSync = Objects.requireNonNull(callback, "callback");
    }

    public void onUnregister(Consumer<UUID> callback) {
        removal = Objects.requireNonNull(callback, "callback");
    }

    public void onFirstCreation(Consumer<Wolf> initializer) {
        firstCreation = Objects.requireNonNull(initializer, "initializer");
    }

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
            commanders.update(id, success.individual().commanderId().orElse(null));
            weaponSync.accept(wolf);
        } else {
            if (loaded.remove(id) != null) removal.accept(id);
            commanders.remove(id);
        }
        return result;
    }

    public void saveAndRegister(Wolf wolf, WonderfulWolfIndividual individual) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(individual, "individual");
        boolean newlyCreated = !store.isWonderful(wolf);
        LoadedIndividual previous = loaded.get(wolf.getUniqueId());
        boolean weaponChanged = previous == null || previous.entity() != wolf
                || !Objects.equals(previous.individual().weapon(), individual.weapon());
        store.save(wolf, individual);
        loaded.put(wolf.getUniqueId(), new LoadedIndividual(wolf, individual));
        commanders.update(wolf.getUniqueId(), individual.commanderId().orElse(null));
        if (newlyCreated) firstCreation.accept(wolf);
        if (weaponChanged) weaponSync.accept(wolf);
    }

    public void unregister(Entity entity) {
        if (!(entity instanceof Wolf wolf)) {
            return;
        }

        LoadedIndividual current = loaded.get(wolf.getUniqueId());
        if (current != null && current.entity() == wolf) {
            loaded.remove(wolf.getUniqueId());
            commanders.remove(wolf.getUniqueId());
            removal.accept(wolf.getUniqueId());
        }
    }

    /** Snapshot only loaded wolves assigned to the requested commander. */
    public Set<UUID> commanderWolfIds(UUID commanderId) {
        return commanders.ids(commanderId);
    }

    public Optional<Wolf> entity(UUID entityId) {
        LoadedIndividual current = loaded.get(Objects.requireNonNull(entityId, "entityId"));
        return current == null ? Optional.empty() : Optional.of(current.entity());
    }

    public Optional<WonderfulWolfIndividual> find(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId");
        LoadedIndividual current = loaded.get(entityId);
        return current == null ? Optional.empty() : Optional.of(current.individual());
    }

    public int size() {
        return loaded.size();
    }

    public List<LoadedSnapshot> snapshots() {
        List<LoadedSnapshot> snapshots = new ArrayList<>(loaded.size());
        for (LoadedIndividual current : loaded.values()) {
            snapshots.add(new LoadedSnapshot(current.entity(), current.individual()));
        }
        return List.copyOf(snapshots);
    }

    public void clear() {
        for (UUID id : loaded.keySet()) removal.accept(id);
        loaded.clear();
        commanders.clear();
    }

    public record LoadedSnapshot(Wolf entity, WonderfulWolfIndividual individual) {
        public LoadedSnapshot {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(individual, "individual");
        }
    }

    private record LoadedIndividual(Wolf entity, WonderfulWolfIndividual individual) {
        private LoadedIndividual {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(individual, "individual");
        }
    }
}
