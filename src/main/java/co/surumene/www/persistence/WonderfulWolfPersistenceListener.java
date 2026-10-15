package co.surumene.www.persistence;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;

public final class WonderfulWolfPersistenceListener implements Listener {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final Logger logger;

    public WonderfulWolfPersistenceListener(
            WonderfulWolfLoadedIndividuals loaded,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        restoreAll(event.getEntities());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        event.getEntities().forEach(loaded::unregister);
    }

    public void restoreAll(Collection<? extends Entity> entities) {
        Objects.requireNonNull(entities, "entities");
        for (Entity entity : entities) {
            RestoreResult result = loaded.register(entity);
            if (result instanceof RestoreResult.Failure failure) {
                logger.warning(
                        "Failed to restore Wonderful Wolf "
                                + entity.getUniqueId()
                                + " ("
                                + failure.reason()
                                + "): "
                                + failure.detail());
            }
        }
    }
}
