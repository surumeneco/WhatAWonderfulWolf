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
    private final Map<UUID, EffectiveAbilities> current = new HashMap<>();
    private final Map<UUID, EvaluationSource> evaluationSources = new HashMap<>();
    private static final int REFRESH_SPAN_TICKS = 20;
    private final ArrayDeque<UUID> incrementalQueue = new ArrayDeque<>();
    private int incrementalTick;
    private int incrementalBudget;

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
        evaluationSources.keySet().retainAll(seen);
    }

    /**
     * Evenly distribute the previous one-second full refresh over 20 ticks.
     * The queue contains UUIDs only: the current entity and Individual are
     * resolved at execution time, never from a stale cached snapshot.
     */
    public void tickIncremental() {
        if (incrementalTick == 0) {
            incrementalQueue.clear();
            for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot : loaded.snapshots()) {
                incrementalQueue.addLast(snapshot.entity().getUniqueId());
            }
            incrementalBudget = perTickBudget(incrementalQueue.size());
        }
        incrementalTick = (incrementalTick + 1) % REFRESH_SPAN_TICKS;

        long now = clock.currentTime();
        WwwConfig.Runtime runtime = Objects.requireNonNull(
                runtimeConfig.get(), "runtime config");
        for (int processed = 0;
                processed < incrementalBudget && !incrementalQueue.isEmpty();
                processed++) {
            UUID id = incrementalQueue.removeFirst();
            loaded.findSnapshot(id)
                    .ifPresent(snapshot -> refreshAt(
                            snapshot.entity(), snapshot.individual(), now, runtime));
        }
        if (incrementalTick == 0) {
            // Include wolves created during this cycle; do not evict their
            // creation-time projected abilities on the basis of an old list.
            current.keySet().removeIf(id -> loaded.find(id).isEmpty());
            evaluationSources.keySet().removeIf(id -> loaded.find(id).isEmpty());
        }
    }

    static int perTickBudget(int count) {
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");
        return Math.max(1, count / REFRESH_SPAN_TICKS
                + (count % REFRESH_SPAN_TICKS == 0 ? 0 : 1));
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
            evaluationSources.remove(wolf.getUniqueId());
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
            evaluationSources.remove(id);
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
                evaluationSources.remove(id);
                return;
            }
        }

        double ageGameDays = wolf.isAdult()
                ? ageGameDays(now, individual.adultBiologicalTime())
                : 0.0;
        try {
            EffectiveAbilities previous = current.get(id);
            EvaluationSource priorSource = evaluationSources.get(id);
            if (previous != null && priorSource != null
                    && priorSource.individual() == individual
                    && priorSource.runtime() == runtime
                    && priorSource.adult() == wolf.isAdult()
                    && sameAgeModifiers(previous, ageGameDays,
                            individual.phenotypeSnapshot().injuries())) {
                // The phenotype and effective age/injury modifiers have not
                // changed. Keep live age accurate without the full decoder.
                EffectiveAbilities unchanged = new EffectiveAbilities(
                        previous.abilities(), ageGameDays, previous.ageCurve());
                projector.project(wolf, unchanged);
                current.put(id, unchanged);
                return;
            }
            EffectiveAbilities abilities =
                    EffectiveAbilityPipeline.evaluate(
                            individual.phenotypeSnapshot(),
                            ageGameDays,
                            runtime,
                            individual.adminAbilityOverrides());
            projector.project(wolf, abilities);
            current.put(id, abilities);
            evaluationSources.put(id, new EvaluationSource(individual, runtime, wolf.isAdult()));
        } catch (RuntimeException error) {
            logger.warning(
                    "Failed to evaluate effective abilities for Wonderful Wolf "
                            + id
                            + ": "
                            + safeMessage(error));
            current.remove(id);
            evaluationSources.remove(id);
        }
    }

    /** Return true iff every age-dependent penalty and injury activation is unchanged. */
    static boolean sameAgeModifiers(
            EffectiveAbilities previous,
            double ageGameDays,
            java.util.List<co.surumene.www.domain.InjuryPhenotype> injuries) {
        if (Double.compare(previous.ageCurve().rankReductionAt(previous.ageGameDays()),
                previous.ageCurve().rankReductionAt(ageGameDays)) != 0) return false;
        for (var injury : injuries) {
            boolean wasActive = previous.ageGameDays() >= injury.onsetGameDay();
            boolean activeNow = ageGameDays >= injury.onsetGameDay();
            if (wasActive != activeNow) return false;
        }
        return true;
    }

    private record EvaluationSource(
            WonderfulWolfIndividual individual, WwwConfig.Runtime runtime, boolean adult) {}

    public Optional<EffectiveAbilities> find(UUID entityId) {
        return Optional.ofNullable(
                current.get(Objects.requireNonNull(entityId, "entityId")));
    }

    public void clear() {
        current.clear();
        evaluationSources.clear();
        incrementalQueue.clear();
        incrementalTick = 0;
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
