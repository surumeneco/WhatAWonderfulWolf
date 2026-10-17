package co.surumene.www.runtime;

import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.ability.EffectiveAbilityPipeline;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.entity.Wolf;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
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
    private static final long AGE_REFRESH_QUANTUM = 200L;

    private final Map<UUID, EffectiveAbilities> current = new HashMap<>();
    private final Map<UUID, WonderfulWolfIndividual> projectedIndividuals = new HashMap<>();
    private final Map<UUID, Long> projectedAges = new HashMap<>();
    private final Set<UUID> dirty = new LinkedHashSet<>();
    private final Set<UUID> juveniles = new HashSet<>();
    private final Map<UUID, Long> nextDue = new HashMap<>();
    private final NavigableMap<Long, Set<UUID>> dueByTime = new TreeMap<>();
    private Runnable workChanged = () -> {};
    public void onWorkChanged(Runnable callback) { workChanged = Objects.requireNonNull(callback); }
    public long nextWakeDelayTicks() {
        return AbilityWakeupPolicy.delay(org.bukkit.Bukkit.getCurrentTick(),
                clock.currentTime(), !bootstrapped || !dirty.isEmpty(),
                juveniles.isEmpty() ? Long.MAX_VALUE : nextJuvenileTick,
                nextCompatibilityTick,
                dueByTime.isEmpty() ? Long.MAX_VALUE : dueByTime.firstKey());
    }
    private boolean bootstrapped;
    private long nextJuvenileTick;
    private long nextCompatibilityTick = 6000L;
    private long previousBiologicalTime = Long.MIN_VALUE;

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
        long now = clock.currentTime();

        // Exactly one startup snapshot, then events and deadlines own updates.
        if (!bootstrapped) {
            bootstrapped = true;
            for (var snapshot : loaded.snapshots()) dirty.add(snapshot.entity().getUniqueId());
        }
        if (previousBiologicalTime != Long.MIN_VALUE && now < previousBiologicalTime) {
            // An administrative biological-time rollback invalidates every age projection.
            markAllDirty();
        }
        previousBiologicalTime = now;

        if (tick >= nextJuvenileTick) {
            nextJuvenileTick = tick + AGE_REFRESH_QUANTUM;
            dirty.addAll(juveniles);
        }
        if (tick >= nextCompatibilityTick) {
            nextCompatibilityTick = tick + 6000L;
            // Exception for eventless third-party edits: maintain a low-frequency audit.
            for (var snapshot : loaded.snapshots()) dirty.add(snapshot.entity().getUniqueId());
        }

        while (!dueByTime.isEmpty() && dueByTime.firstKey() <= now) {
            var first = dueByTime.pollFirstEntry();
            for (UUID id : first.getValue()) {
                if (nextDue.remove(id, first.getKey())) dirty.add(id);
            }
        }

        if (dirty.isEmpty()) return;
        WwwConfig.Runtime runtime =
                Objects.requireNonNull(runtimeConfig.get(), "runtime config");
        // Snapshot a bounded batch first: refreshAt may save the adulthood
        // timestamp, which invokes markDirty and must not invalidate an iterator.
        List<UUID> batch = new ArrayList<>(MAX_REFRESHES_PER_TICK);
        Iterator<UUID> queue = dirty.iterator();
        while (queue.hasNext() && batch.size() < MAX_REFRESHES_PER_TICK) {
            UUID id = queue.next();
            queue.remove();
            batch.add(id);
        }
        for (UUID id : batch) {
            Wolf wolf = loaded.entity(id).orElse(null);
            WonderfulWolfIndividual latest = loaded.find(id).orElse(null);
            if (wolf == null || latest == null || !wolf.isValid() || wolf.isDead()) {
                untrack(id);
                continue;
            }
            refreshAt(wolf, latest, now, runtime);
        }
    }

    /** Mutation callbacks enqueue an ID, never a potentially stale Bukkit entity. */
    public void markDirty(Wolf wolf) {
        dirty.add(Objects.requireNonNull(wolf, "wolf").getUniqueId());
        workChanged.run();
    }

    /** Evict all transient ability work when an entity unloads. */
    public void untrack(UUID id) {
        dirty.remove(id);
        juveniles.remove(id);
        current.remove(id);
        projectedIndividuals.remove(id);
        projectedAges.remove(id);
        unschedule(id);
    }

    private void unschedule(UUID id) {
        Long due = nextDue.remove(id);
        if (due == null) return;
        Set<UUID> ids = dueByTime.get(due);
        if (ids != null) {
            ids.remove(id);
            if (ids.isEmpty()) dueByTime.remove(due);
        }
    }

    private void schedule(UUID id, long due) {
        unschedule(id);
        if (due == Long.MAX_VALUE) return;
        nextDue.put(id, due);
        dueByTime.computeIfAbsent(due, unused -> new HashSet<>()).add(id);
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
            if (wolf.isAdult()) {
                juveniles.remove(id);
                long due = AgeProjectionDeadline.next(
                        now, individual.adultBiologicalTime(), abilities.ageCurve(),
                        individual.phenotypeSnapshot().injuries().stream()
                                .map(injury -> injury.onsetGameDay()).toList());
                schedule(id, due);
            } else {
                juveniles.add(id);
                unschedule(id);
            }
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

    /** Invalidate cached projections after runtime configuration reload. */
    public void markAllDirty() {
        dueByTime.clear();
        nextDue.clear();
        dirty.clear();
        for (var snapshot : loaded.snapshots()) dirty.add(snapshot.entity().getUniqueId());
        workChanged.run();
    }

    public void clear() {
        current.clear();
        projectedIndividuals.clear();
        projectedAges.clear();
        dirty.clear();
        juveniles.clear();
        nextDue.clear();
        dueByTime.clear();
        bootstrapped = false;
        nextJuvenileTick = 0L;
        nextCompatibilityTick = 6000L;
        previousBiologicalTime = Long.MIN_VALUE;
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
