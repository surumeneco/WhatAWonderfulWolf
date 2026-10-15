package co.surumene.www.runtime;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class WonderfulWolfTraitRuntime {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final Supplier<WwwConfig.Runtime> config;
    private final WonderfulWolfStatusEffectRuntime statusEffects;
    private final WonderfulWolfTriggeredEffectRuntime triggeredEffects;
    private final WonderfulWolfWatchmanRuntime watchman;
    private final WonderfulWolfScavengerRuntime scavenger;
    private final Logger logger;

    public WonderfulWolfTraitRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfAbilityRuntime abilities,
            WonderfulWolfBehaviorRuntime behavior,
            Supplier<WwwConfig.Runtime> config,
            Server server,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.config = Objects.requireNonNull(config, "config");
        this.statusEffects = new WonderfulWolfStatusEffectRuntime();
        this.triggeredEffects =
                new WonderfulWolfTriggeredEffectRuntime(server);
        this.watchman = new WonderfulWolfWatchmanRuntime(
                loaded,
                behavior,
                server);
        this.scavenger = new WonderfulWolfScavengerRuntime(
                loaded,
                abilities,
                server);
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void tick(long serverTick) {
        WwwConfig.Runtime settings =
                Objects.requireNonNull(config.get(), "runtime config");
        Set<UUID> active = new HashSet<>();

        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            Wolf wolf = snapshot.entity();
            active.add(wolf.getUniqueId());
            if (wolf.isDead() || !wolf.isValid()) {
                continue;
            }

            try {
                WonderfulWolfIndividual individual =
                        snapshot.individual();
                statusEffects.tick(
                        wolf,
                        individual,
                        settings);
                watchman.tick(
                        wolf,
                        individual,
                        settings,
                        serverTick);
                scavenger.tick(
                        wolf,
                        individual);
            } catch (RuntimeException error) {
                logger.warning(
                        "Failed to update Wonderful Wolf traits for "
                                + wolf.getUniqueId()
                                + ": "
                                + safeMessage(error));
            }
        }

        watchman.retain(active);
        triggeredEffects.tick(serverTick);
    }

    public boolean isImmune(
            Wolf wolf,
            EffectPolicy.DamageKind kind) {
        WonderfulWolfIndividual individual =
                loaded.find(wolf.getUniqueId()).orElse(null);
        return individual != null
                && individual.phenotypeSnapshot()
                        .expressedTraits()
                        .stream()
                        .anyMatch(entry ->
                                EffectPolicy.immuneTo(
                                        entry.trait(),
                                        kind));
    }

    public void onHit(
            Wolf wolf,
            LivingEntity target,
            long serverTick) {
        WonderfulWolfIndividual individual =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (individual != null && !target.equals(wolf)) {
            triggeredEffects.onHit(
                    individual,
                    target,
                    serverTick);
        }
    }

    public void onKill(Wolf wolf) {
        loaded.find(wolf.getUniqueId()).ifPresent(individual ->
                triggeredEffects.onKill(
                        individual,
                        wolf));
    }

    public void clear() {
        triggeredEffects.clear();
        watchman.clear();
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName()
                        + ": "
                        + message;
    }
}
