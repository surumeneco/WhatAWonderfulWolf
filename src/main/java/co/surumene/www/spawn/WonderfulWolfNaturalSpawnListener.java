package co.surumene.www.spawn;

import co.surumene.www.config.WwwConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class WonderfulWolfNaturalSpawnListener implements Listener {
    private final NaturalWolfConverter converter;
    private final Supplier<WwwConfig.Spawn> spawnConfig;
    private final DoubleSupplier probabilityDraw;
    private final LongSupplier seedSupplier;
    private final Logger logger;

    public WonderfulWolfNaturalSpawnListener(
            NaturalWolfConverter converter,
            Supplier<WwwConfig.Spawn> spawnConfig,
            DoubleSupplier probabilityDraw,
            LongSupplier seedSupplier,
            Logger logger) {
        this.converter = Objects.requireNonNull(converter, "converter");
        this.spawnConfig = Objects.requireNonNull(spawnConfig, "spawnConfig");
        this.probabilityDraw = Objects.requireNonNull(probabilityDraw, "probabilityDraw");
        this.seedSupplier = Objects.requireNonNull(seedSupplier, "seedSupplier");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)
                || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        double probability = currentProbability();
        if (!NaturalWolfSpawnPolicy.shouldConvert(
                event.getSpawnReason(),
                probability,
                probabilityDraw.getAsDouble())) {
            return;
        }
        convert(wolf);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }

        double probability = currentProbability();
        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof Wolf wolf)) {
                continue;
            }
            if (NaturalWolfSpawnPolicy.shouldConvertGeneratedWolf(
                    true,
                    probability,
                    probabilityDraw.getAsDouble())) {
                convert(wolf);
            }
        }
    }

    private double currentProbability() {
        return Objects.requireNonNull(
                spawnConfig.get(),
                "spawn config").naturalConversionProbability();
    }

    private void convert(Wolf wolf) {
        try {
            WonderfulWolfEntityCreationResult result =
                    converter.convert(wolf, seedSupplier.getAsLong());
            if (result instanceof WonderfulWolfEntityCreationResult.Failure failure) {
                logger.warning(
                        "Wonderful Wolf natural conversion failed for "
                                + wolf.getUniqueId()
                                + " ("
                                + failure.reason()
                                + "): "
                                + failure.detail());
            }
        } catch (RuntimeException error) {
            logger.warning(
                    "Wonderful Wolf natural conversion failed unexpectedly for "
                            + wolf.getUniqueId()
                            + ": "
                            + safeMessage(error));
        }
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
