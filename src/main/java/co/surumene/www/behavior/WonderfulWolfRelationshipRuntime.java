package co.surumene.www.behavior;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.Mode;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private final Map<UUID, Long> lastPassiveCheck = new HashMap<>();

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
    }

    public void tick(long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must be >= 0");
        }
        pendingFeeds.cleanup(serverTick);

        WwwConfig.Relationship settings =
                Objects.requireNonNull(config.get(), "relationship config");
        Set<UUID> seen = new HashSet<>();

        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            UUID wolfId = snapshot.entity().getUniqueId();
            seen.add(wolfId);

            if ((snapshot.individual().mode() != Mode.FOLLOW
                    && snapshot.individual().mode() != Mode.GUARD)
                    || snapshot.individual().commanderId().isEmpty()) {
                lastPassiveCheck.remove(wolfId);
                continue;
            }

            long previous = lastPassiveCheck.getOrDefault(
                    wolfId,
                    serverTick);
            lastPassiveCheck.putIfAbsent(wolfId, serverTick);
            if (serverTick - previous
                    < settings.passiveIncreaseIntervalTicks()) {
                continue;
            }
            lastPassiveCheck.put(wolfId, serverTick);

            if (draw.getAsDouble()
                    >= settings.passiveIncreaseProbability()) {
                continue;
            }
            try {
                relationships.reward(
                        snapshot.entity(),
                        snapshot.individual().commanderId().orElseThrow());
            } catch (RuntimeException error) {
                logger.warning(
                        "Failed to persist passive affection gain for Wonderful Wolf "
                                + wolfId
                                + ": "
                                + safeMessage(error));
            }
        }

        lastPassiveCheck.keySet().retainAll(seen);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
