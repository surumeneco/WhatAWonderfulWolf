package co.surumene.www.behavior;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.Mode;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.entity.Wolf;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class WonderfulWolfRelationshipRuntime {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfRelationshipService relationships;
    private final Supplier<WwwConfig.Relationship> config;
    private final DoubleSupplier draw;
    private final PendingFeedTracker pendingFeeds;
    private final Logger logger;
    private final PassiveAffectionSchedule passiveSchedule =
            new PassiveAffectionSchedule();
    private final PassiveAffectionDeadlineQueue deadlineQueue =
            new PassiveAffectionDeadlineQueue();
    private boolean bootstrapped;

    public WonderfulWolfRelationshipRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfRelationshipService relationships,
            Supplier<WwwConfig.Relationship> config,
            DoubleSupplier draw,
            PendingFeedTracker pendingFeeds,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.relationships = Objects.requireNonNull(relationships, "relationships");
        this.config = Objects.requireNonNull(config, "config");
        this.draw = Objects.requireNonNull(draw, "draw");
        this.pendingFeeds = Objects.requireNonNull(pendingFeeds, "pendingFeeds");
        this.logger = Objects.requireNonNull(logger, "logger");
        loaded.onPassiveAffectionChanged(this::onStateChanged);
    }

    /** A mutation changes only the affected wolf's work; it does not re-roll. */
    private void onStateChanged(UUID id, boolean entityReplaced) {
        if (entityReplaced) {
            // Never apply an old incarnation's timing to a new Entity.
            passiveSchedule.clear(id);
        }
        if (loaded.find(id).isEmpty()) {
            passiveSchedule.clear(id);
            deadlineQueue.discard(id);
        } else {
            deadlineQueue.changed(id);
        }
    }

    /** Config reload may change the interval without resetting elapsed time. */
    public void onConfigReload() {
        for (var snapshot : loaded.passiveAffectionSnapshots()) {
            deadlineQueue.changed(snapshot.entity().getUniqueId());
        }
    }

    public void tick(long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must be >= 0");
        }
        if (!bootstrapped) {
            bootstrapped = true;
            for (var snapshot : loaded.passiveAffectionSnapshots()) {
                deadlineQueue.changed(snapshot.entity().getUniqueId());
            }
        }
        Set<UUID> candidates = deadlineQueue.takeReady(serverTick);
        if (candidates.isEmpty()) {
            return;
        }

        WwwConfig.Relationship settings =
                Objects.requireNonNull(config.get(), "relationship config");
        for (UUID id : candidates) {
            Wolf wolf = loaded.entity(id).orElse(null);
            WonderfulWolfIndividual individual = loaded.find(id).orElse(null);
            if (wolf == null || individual == null
                    || (individual.mode() != Mode.FOLLOW
                            && individual.mode() != Mode.GUARD)
                    || individual.commanderId().isEmpty()) {
                passiveSchedule.clear(id);
                deadlineQueue.discard(id);
                continue;
            }

            UUID commanderId = individual.commanderId().orElseThrow();
            if (passiveSchedule.isDue(
                    id, commanderId, serverTick,
                    settings.passiveIncreaseIntervalTicks())
                    && draw.getAsDouble() < settings.passiveIncreaseProbability()) {
                try {
                    relationships.reward(wolf, commanderId);
                } catch (RuntimeException error) {
                    logger.warning(
                            "Failed to persist passive affection gain for Wonderful Wolf "
                                    + id + ": " + safeMessage(error));
                }
            }

            deadlineQueue.schedule(
                    id,
                    passiveSchedule.nextCheckTick(
                            id, settings.passiveIncreaseIntervalTicks()));
        }
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
