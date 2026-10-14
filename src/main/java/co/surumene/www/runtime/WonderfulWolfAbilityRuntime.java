package co.surumene.www.runtime;

import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.ability.EffectiveAbilityPipeline;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.entity.Wolf;

import java.util.ArrayDeque;
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
    private static final int MAX_REFRESHES_PER_TICK = 16;
    private static final long AGE_REFRESH_QUANTUM = 1_200L;

    private final Map<UUID, EffectiveAbilities> current = new HashMap<>();
    private final Map<UUID, WonderfulWolfIndividual> projectedIndividuals = new HashMap<>();
    private final Map<UUID, Long> projectedAges = new HashMap<>();
    private final ArrayDeque<WonderfulWolfLoadedIndividuals.LoadedSnapshot> pending = new ArrayDeque<>();
    private long nextScanTick;

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

    /**
     * Incremental maintenance rather than reevaluating every loaded individual
     * on one server tick. Explicit edits still use refresh(Wolf) immediately.
     */
    public void tick() {
        long tick = org.bukkit.Bukkit.getCurrentTick();
        if (pending.isEmpty()) {
            if (tick < nextScanTick) return;
            nextScanTick = tick + 20L;
            var snapshots = loaded.snapshots();
            Set<UUID> seen = new HashSet<>(snapshots.size());
            for (var snapshot : snapshots) seen.add(snapshot.entity().getUniqueId());
            current.keySet().retainAll(seen);
            projectedIndividuals.keySet().retainAll(seen);
            projectedAges.keySet().retainAll(seen);
            pending.addAll(snapshots);
        }

        long now = clock.currentTime();
        WwwConfig.Runtime runtime = Objects.requireNonNull(
                runtimeConfig.get(), "runtime config");
        for (int i = 0; i < MAX_REFRESHES_PER_TICK && !pending.isEmpty(); i++) {
            var snapshot = pending.removeFirst();
            Wolf wolf = snapshot.entity();
            UUID id = wolf.getUniqueId();
            // A newer on-demand edit can supersede a snapshot captured earlier.
            WonderfulWolfIndividual latest = loaded.find(id).orElse(null);
            if (latest == null || !wolf.isValid() || wolf.isDead()) {
                current.remove(id);
                projectedIndividuals.remove(id);
                projectedAges.remove(id);
                continue;
            }

            long ageBucket = now / AGE_REFRESH_QUANTUM;
            if (current.containsKey(id)
                    && projectedIndividuals.get(id) == latest
                    && projectedAges.getOrDefault(id, Long.MIN_VALUE) == ageBucket) {
                continue;
            }
            refreshAt(wolf, latest, now, runtime);
        }
    }

    /**
     * Initialize a newly created WWW entity after its Individual has been registered.
     * Never call this during PDC restoration, routine refresh, or administrative edits.
     */
    public void initializeNew(Wolf wolf) {
        Objects.requireNonNull(wolf, "wolf");
        refresh(wolf);
        if (current.containsKey(wolf.getUniqueId()) && wolf.isValid() && !wolf.isDead()) {
            org.bukkit.attribute.AttributeInstance maxHealth =
                    wolf.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealth != null && maxHealth.getValue() > 0.0) {
                wolf.setHealth(maxHealth.getValue());
            }
        }
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
        if (!wolf.isValid() || wolf.isDead()) {
            current.remove(id);
            return;
        }

        if (wolf.isAdult() && individual.adultBiologicalTime() == 0L && now > 0L) {
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

        double ageGameDays = wolf.isAdult()
                ? ageGameDays(now, individual.adultBiologicalTime())
                : 0.0;
        try {
            EffectiveAbilities abilities =
                    EffectiveAbilityPipeline.evaluate(
                            individual.phenotypeSnapshot(),
                            ageGameDays,
                            runtime,
                            individual.adminAbilityOverrides());
            projector.project(wolf, abilities);
            current.put(id, abilities);
            // The adulthood timestamp can be initialized during refreshAt.
            projectedIndividuals.put(id, individual);
            projectedAges.put(id, now / AGE_REFRESH_QUANTUM);
        } catch (RuntimeException error) {
            logger.warning(
                    "Failed to evaluate effective abilities for Wonderful Wolf "
                            + id
                            + ": "
                            + safeMessage(error));
            current.remove(id);
            projectedIndividuals.remove(id);
            projectedAges.remove(id);
        }
    }

    public Optional<EffectiveAbilities> find(UUID entityId) {
        return Optional.ofNullable(
                current.get(Objects.requireNonNull(entityId, "entityId")));
    }

    public void clear() {
        current.clear();
        projectedIndividuals.clear();
        projectedAges.clear();
        pending.clear();
        nextScanTick = 0L;
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
