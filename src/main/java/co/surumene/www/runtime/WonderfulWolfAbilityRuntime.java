package co.surumene.www.runtime;

import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.ability.EffectiveAbilityPipeline;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.entity.Wolf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class WonderfulWolfAbilityRuntime {
    public static final double TICKS_PER_GAME_DAY = 24_000.0;

    private final WonderfulWolfLoadedIndividuals loaded;
    private final BiologicalClock clock;
    private final Supplier<WwwConfig.Runtime> runtimeConfig;
    private final PaperAbilityProjector projector;
    private final Logger logger;
    private final Map<UUID, EffectiveAbilities> current = new HashMap<>();

    public WonderfulWolfAbilityRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            BiologicalClock clock,
            Supplier<WwwConfig.Runtime> runtimeConfig,
            PaperAbilityProjector projector,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.runtimeConfig = Objects.requireNonNull(runtimeConfig, "runtimeConfig");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void tick() {
        long now = clock.currentTime();
        WwwConfig.Runtime runtime = Objects.requireNonNull(
                runtimeConfig.get(), "runtime config");
        Set<UUID> seen = new HashSet<>();

        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            Wolf wolf = snapshot.entity();
            seen.add(wolf.getUniqueId());
            refreshAt(wolf, snapshot.individual(), now, runtime);
        }

        current.keySet().retainAll(seen);
    }

    public void refresh(Wolf wolf) {
        Objects.requireNonNull(wolf, "wolf");
        WonderfulWolfIndividual individual = loaded.find(wolf.getUniqueId())
                .orElse(null);
        if (individual == null) {
            current.remove(wolf.getUniqueId());
            return;
        }
        refreshAt(
                wolf,
                individual,
                clock.currentTime(),
                Objects.requireNonNull(runtimeConfig.get(), "runtime config"));
    }

    private void refreshAt(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            long now,
            WwwConfig.Runtime runtime) {
        UUID id = wolf.getUniqueId();
        if (!wolf.isValid() || !wolf.isAdult()) {
            current.remove(id);
            return;
        }

        if (individual.adultBiologicalTime() == 0L && now > 0L) {
            individual = individual.withAdultBiologicalTime(now);
            try {
                loaded.saveAndRegister(wolf, individual);
            } catch (RuntimeException error) {
                logger.warning(
                        "Failed to persist adult biological time for Wonderful Wolf "
                                + id
                                + ": "
                                + safeMessage(error));
                current.remove(id);
                return;
            }
        }

        double ageGameDays = ageGameDays(
                now,
                individual.adultBiologicalTime());
        try {
            EffectiveAbilities abilities =
                    EffectiveAbilityPipeline.evaluate(
                            individual.phenotypeSnapshot(),
                            ageGameDays,
                            runtime);
            projector.project(wolf, abilities);
            current.put(id, abilities);
        } catch (RuntimeException error) {
            logger.warning(
                    "Failed to evaluate effective abilities for Wonderful Wolf "
                            + id
                            + ": "
                            + safeMessage(error));
            current.remove(id);
        }
    }

    public Optional<EffectiveAbilities> find(UUID entityId) {
        return Optional.ofNullable(
                current.get(Objects.requireNonNull(entityId, "entityId")));
    }

    public void clear() {
        current.clear();
    }

    public static double ageGameDays(
            long currentBiologicalTime,
            long adultBiologicalTime) {
        if (currentBiologicalTime < 0L || adultBiologicalTime < 0L) {
            throw new IllegalArgumentException(
                    "biological times must be >= 0");
        }
        long elapsed = Math.max(
                0L,
                currentBiologicalTime - adultBiologicalTime);
        return elapsed / TICKS_PER_GAME_DAY;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
