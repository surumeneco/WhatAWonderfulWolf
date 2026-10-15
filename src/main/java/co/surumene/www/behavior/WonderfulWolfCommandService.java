package co.surumene.www.behavior;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.individual.WorldPosition;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Location;
import org.bukkit.entity.Wolf;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class WonderfulWolfCommandService {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final ManualTargetRegistry manualTargets;
    private final Consumer<Wolf> stateChanged;

    public WonderfulWolfCommandService(
            WonderfulWolfLoadedIndividuals loaded,
            ManualTargetRegistry manualTargets,
            Consumer<Wolf> stateChanged) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.manualTargets = Objects.requireNonNull(manualTargets, "manualTargets");
        this.stateChanged = Objects.requireNonNull(stateChanged, "stateChanged");
    }

    public boolean changeMode(
            Wolf wolf,
            Mode mode,
            UUID actorId) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(actorId, "actorId");
        WonderfulWolfIndividual current =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (current == null) {
            return false;
        }

        Location location = wolf.getLocation();
        WorldPosition position = new WorldPosition(
                wolf.getWorld().getUID(),
                location.getX(),
                location.getY(),
                location.getZ());
        WonderfulWolfIndividual updated =
                CommandStatePolicy.changeMode(
                        current,
                        mode,
                        actorId,
                        position);
        loaded.saveAndRegister(wolf, updated);

        if (mode == Mode.WANDER) {
            manualTargets.clear(wolf.getUniqueId());
        }
        wolf.setTarget(null);
        wolf.getPathfinder().stopPathfinding();
        stateChanged.accept(wolf);
        return true;
    }

    public boolean changeActionDistance(
            Wolf wolf,
            ActionDistance actionDistance) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(actionDistance, "actionDistance");
        WonderfulWolfIndividual current =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (current == null) {
            return false;
        }

        WonderfulWolfIndividual updated =
                CommandStatePolicy.changeActionDistance(
                        current,
                        actionDistance);
        loaded.saveAndRegister(wolf, updated);
        stateChanged.accept(wolf);
        return true;
    }
}
