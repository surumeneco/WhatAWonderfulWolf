package co.surumene.www.runtime;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.Trait;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.individual.WorldPosition;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class WonderfulWolfWatchmanRuntime {
    private static final int GLOW_TICKS = 40;
    private static final int GLOW_REFRESH_THRESHOLD = 20;

    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfBehaviorRuntime behavior;
    private final Server server;
    private final Map<UUID, Long> lastAlert = new HashMap<>();

    WonderfulWolfWatchmanRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfBehaviorRuntime behavior,
            Server server) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.server = Objects.requireNonNull(server, "server");
    }

    void tick(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            WwwConfig.Runtime settings,
            long serverTick) {
        applySelectedTargetGlow(wolf, individual);
        applyWatchman(wolf, individual, settings, serverTick);
    }

    void retain(Set<UUID> activeWolfIds) {
        lastAlert.keySet().retainAll(activeWolfIds);
    }

    void clear() {
        lastAlert.clear();
    }

    private void applySelectedTargetGlow(
            Wolf wolf,
            WonderfulWolfIndividual individual) {
        if (!ExpressedTraitLookup.has(individual, Trait.TARGET)) {
            return;
        }
        behavior.selectedTarget(wolf.getUniqueId()).ifPresent(target ->
                WonderfulWolfStatusEffectRuntime.applyPotion(
                        target,
                        PotionEffectType.GLOWING,
                        GLOW_TICKS,
                        0,
                        GLOW_REFRESH_THRESHOLD));
    }

    private void applyWatchman(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            WwwConfig.Runtime settings,
            long serverTick) {
        UUID wolfId = wolf.getUniqueId();
        if (!ExpressedTraitLookup.has(individual, Trait.WATCHMAN)) {
            lastAlert.remove(wolfId);
            return;
        }

        Location reference = referenceLocation(wolf, individual);
        if (reference == null || reference.getWorld() == null) {
            lastAlert.remove(wolfId);
            return;
        }

        double radius = individual.actionDistance()
                .blocks(settings.actionDistance())
                * settings.traits().watchmanDistanceMultiplier();
        if (!(radius > 0.0)) {
            lastAlert.remove(wolfId);
            return;
        }

        Set<UUID> engaged = engagedTargets(wolfId, individual);
        double radiusSquared = radius * radius;
        double nearest = Double.POSITIVE_INFINITY;

        for (Entity entity : reference.getWorld().getNearbyEntities(
                reference,
                radius,
                radius,
                radius)) {
            if (!(entity instanceof LivingEntity living)
                    || !(living instanceof Enemy)
                    || living instanceof Player
                    || living.isDead()
                    || !living.isValid()
                    || engaged.contains(living.getUniqueId())
                    || reference.distanceSquared(living.getLocation())
                        > radiusSquared) {
                continue;
            }

            WonderfulWolfStatusEffectRuntime.applyPotion(
                    living,
                    PotionEffectType.GLOWING,
                    GLOW_TICKS,
                    0,
                    GLOW_REFRESH_THRESHOLD);
            nearest = Math.min(
                    nearest,
                    reference.distance(living.getLocation()));
        }

        if (!Double.isFinite(nearest)) {
            lastAlert.remove(wolfId);
            return;
        }

        long interval = EffectPolicy.watchmanInterval(
                nearest,
                radius,
                settings.traits().watchmanWarningMinTicks(),
                settings.traits().watchmanWarningMaxTicks());
        long previous =
                lastAlert.getOrDefault(wolfId, Long.MIN_VALUE / 2);
        if (serverTick - previous < interval) {
            return;
        }

        wolf.getWorld().playSound(
                wolf.getLocation(),
                Sound.ENTITY_WOLF_GROWL,
                1.0f,
                1.1f);
        lastAlert.put(wolfId, serverTick);
    }

    private Set<UUID> engagedTargets(
            UUID currentWolfId,
            WonderfulWolfIndividual individual) {
        Set<UUID> result = new HashSet<>();
        behavior.selectedTarget(currentWolfId)
                .map(Entity::getUniqueId)
                .ifPresent(result::add);

        UUID commanderId = individual.commanderId().orElse(null);
        if (commanderId == null) {
            return result;
        }

        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            if (snapshot.individual().commanderId()
                    .filter(commanderId::equals)
                    .isEmpty()) {
                continue;
            }
            behavior.selectedTarget(snapshot.entity().getUniqueId())
                    .map(Entity::getUniqueId)
                    .ifPresent(result::add);
        }
        return result;
    }

    private Location referenceLocation(
            Wolf wolf,
            WonderfulWolfIndividual individual) {
        if (individual.mode() == Mode.WANDER) {
            return wolf.getLocation();
        }
        if (individual.mode() == Mode.WAIT) {
            WorldPosition wait = individual.waitLocation().orElse(null);
            if (wait == null) {
                return null;
            }
            World world = server.getWorld(wait.worldId());
            return world == null
                    ? null
                    : new Location(
                            world,
                            wait.x(),
                            wait.y(),
                            wait.z());
        }

        Player commander = individual.commanderId()
                .map(server::getPlayer)
                .orElse(null);
        return commander == null || !commander.isOnline()
                ? null
                : commander.getLocation();
    }
}
