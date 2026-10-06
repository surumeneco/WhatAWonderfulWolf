package co.surumene.www.behavior;

import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

public final class WonderfulWolfRelationshipListener implements Listener {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfRelationshipService relationships;
    private final PendingFeedTracker pendingFeeds;
    private final LongSupplier currentTick;
    private final Logger logger;

    public WonderfulWolfRelationshipListener(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfRelationshipService relationships,
            PendingFeedTracker pendingFeeds,
            LongSupplier currentTick,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.relationships = Objects.requireNonNull(relationships, "relationships");
        this.pendingFeeds = Objects.requireNonNull(pendingFeeds, "pendingFeeds");
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Wolf wolf)
                || loaded.find(wolf.getUniqueId()).isEmpty()) {
            return;
        }
        pendingFeeds.record(
                wolf.getUniqueId(),
                event.getPlayer().getUniqueId(),
                currentTick.getAsLong());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)
                || event.getAmount() <= 0.0
                || loaded.find(wolf.getUniqueId()).isEmpty()) {
            return;
        }

        pendingFeeds.consume(wolf.getUniqueId(), currentTick.getAsLong())
                .ifPresent(playerId -> {
                    try {
                        relationships.reward(wolf, playerId);
                    } catch (RuntimeException error) {
                        warn("feeding affection", wolf, error);
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDirectPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)
                || !(event.getDamager() instanceof Player player)
                || event.getFinalDamage() <= 0.0
                || loaded.find(wolf.getUniqueId()).isEmpty()) {
            return;
        }
        try {
            relationships.penalize(wolf, player.getUniqueId());
        } catch (RuntimeException error) {
            warn("damage affection", wolf, error);
        }
    }

    private void warn(
            String operation,
            Wolf wolf,
            RuntimeException error) {
        logger.warning(
                "Failed to persist "
                        + operation
                        + " for Wonderful Wolf "
                        + wolf.getUniqueId()
                        + ": "
                        + safeMessage(error));
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
