package co.surumene.www.wolftrap;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfEntityCreationResult;
import io.papermc.paper.world.MoonPhase;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class WolfTrapRuntime {
    private final WolfTrapStateStore states;
    private final PaperWonderfulWolfFactory wolves;
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfBehaviorRuntime behavior;
    private final Supplier<WwwConfig.Runtime> config;
    private final Server server;
    private final DoubleSupplier random;
    private final LongSupplier seeds;
    private final LongSupplier wallClockMillis;
    private final Logger logger;
    private final java.util.Map<UUID, Long> previousFullTimes =
            new java.util.HashMap<>();
    private final Set<UUID> armedEvokers = new HashSet<>();
    private final Set<UUID> riderEvokers = new HashSet<>();
    private boolean creatingOwnLightning;

    public WolfTrapRuntime(
            WolfTrapStateStore states,
            PaperWonderfulWolfFactory wolves,
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfBehaviorRuntime behavior,
            Supplier<WwwConfig.Runtime> config,
            Server server,
            DoubleSupplier random,
            LongSupplier seeds,
            LongSupplier wallClockMillis,
            Logger logger) {
        this.states = Objects.requireNonNull(states, "states");
        this.wolves = Objects.requireNonNull(wolves, "wolves");
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.config = Objects.requireNonNull(config, "config");
        this.server = Objects.requireNonNull(server, "server");
        this.random = Objects.requireNonNull(random, "random");
        this.seeds = Objects.requireNonNull(seeds, "seeds");
        this.wallClockMillis =
                Objects.requireNonNull(wallClockMillis, "wallClockMillis");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void restoreLoaded() {
        armedEvokers.clear();
        riderEvokers.clear();
        for (World world : server.getWorlds()) {
            previousFullTimes.put(world.getUID(), world.getFullTime());
            for (Evoker evoker : world.getEntitiesByClass(Evoker.class)) {
                register(evoker);
            }
            for (Wolf wolf : world.getEntitiesByClass(Wolf.class)) {
                restoreMountedWolf(wolf);
            }
        }
    }

    public void register(Entity entity) {
        if (entity instanceof Evoker evoker) {
            if (states.isArmed(evoker)) {
                armedEvokers.add(evoker.getUniqueId());
            } else if (states.isRider(evoker)) {
                riderEvokers.add(evoker.getUniqueId());
            }
        } else if (entity instanceof Wolf wolf) {
            restoreMountedWolf(wolf);
        }
    }

    public void tick() {
        tickAdditionalSpawns();
        tickArmedEvokers();
        tickRiders();
    }

    public void onLightning(LightningStrikeEvent event) {
        Objects.requireNonNull(event, "event");
        if (creatingOwnLightning
                || !WolfTrapPolicy.isNaturalLightning(event.getCause())) {
            return;
        }

        double probability = Objects.requireNonNull(
                config.get(),
                "runtime config")
                .spawn()
                .wolfTrapLightningProbability();
        if (!WolfTrapPolicy.roll(draw(), probability)) {
            return;
        }
        spawnArmed(event.getLightning().getLocation(), 0L);
    }

    public void activateByAttack(Evoker evoker) {
        if (evoker != null && states.isArmed(evoker)) {
            activate(evoker);
        }
    }

    public void onEvokerDeath(Evoker evoker) {
        if (evoker == null) {
            return;
        }
        armedEvokers.remove(evoker.getUniqueId());
        riderEvokers.remove(evoker.getUniqueId());

        if (!states.isRider(evoker)) {
            states.clearArmed(evoker);
            return;
        }

        Entity vehicle = evoker.getVehicle();
        states.clearRider(evoker);
        if (vehicle instanceof Wolf wolf) {
            states.clearMountedWolf(wolf);
            behavior.clearTrapRiderTarget(wolf);
        }
    }

    public void onWolfDeath(Wolf wolf) {
        if (wolf == null || !states.isMountedWolf(wolf)) {
            return;
        }
        states.clearMountedWolf(wolf);
        for (Entity passenger : List.copyOf(wolf.getPassengers())) {
            if (passenger instanceof Evoker evoker
                    && states.isRider(evoker)) {
                states.clearRider(evoker);
                riderEvokers.remove(evoker.getUniqueId());
            }
        }
        behavior.clearTrapRiderTarget(wolf);
    }

    private void tickAdditionalSpawns() {
        for (World world : server.getWorlds()) {
            long current = world.getFullTime();
            Long previous =
                    previousFullTimes.put(world.getUID(), current);
            if (previous == null
                    || !WolfTrapPolicy.reachedAdditionalCheckTime(
                            previous,
                            current)
                    || world.getMoonPhase() != MoonPhase.FULL_MOON
                    || !world.hasStorm()) {
                continue;
            }

            List<Player> players = world.getPlayers();
            if (players.isEmpty()
                    || !WolfTrapPolicy.roll(
                            draw(),
                            WolfTrapPolicy.ADDITIONAL_PROBABILITY)) {
                continue;
            }

            Player selected = players.get(
                    Math.min(
                            players.size() - 1,
                            (int) Math.floor(draw() * players.size())));
            Location location = randomSkyLocation(selected);
            strikeOwnLightning(location);
            spawnArmed(
                    location,
                    wallClockMillis.getAsLong()
                            + WolfTrapPolicy.ADDITIONAL_GRACE_MILLIS);
        }
    }

    private void tickArmedEvokers() {
        double radius = Objects.requireNonNull(
                config.get(),
                "runtime config")
                .spawn()
                .wolfTrapActivationRadiusBlocks();
        double radiusSquared = radius * radius;
        long now = wallClockMillis.getAsLong();

        for (UUID id : List.copyOf(armedEvokers)) {
            Entity entity = server.getEntity(id);
            if (!(entity instanceof Evoker evoker)
                    || evoker.isDead()
                    || !evoker.isValid()
                    || !states.isArmed(evoker)) {
                armedEvokers.remove(id);
                continue;
            }
            if (now < states.graceUntil(evoker)) {
                continue;
            }

            Location location = evoker.getLocation();
            for (Player player : evoker.getWorld().getPlayers()) {
                if (!player.isDead()
                        && player.isValid()
                        && location.distanceSquared(player.getLocation())
                            <= radiusSquared) {
                    activate(evoker);
                    break;
                }
            }
        }
    }

    private void tickRiders() {
        for (UUID id : List.copyOf(riderEvokers)) {
            Entity entity = server.getEntity(id);
            if (!(entity instanceof Evoker evoker)
                    || evoker.isDead()
                    || !evoker.isValid()
                    || !states.isRider(evoker)) {
                riderEvokers.remove(id);
                continue;
            }

            Entity vehicle = evoker.getVehicle();
            if (!(vehicle instanceof Wolf wolf)
                    || !states.isMountedWolf(wolf)
                    || loaded.find(wolf.getUniqueId()).isEmpty()) {
                states.clearRider(evoker);
                riderEvokers.remove(id);
                if (vehicle instanceof Wolf wolfVehicle) {
                    states.clearMountedWolf(wolfVehicle);
                    behavior.clearTrapRiderTarget(wolfVehicle);
                }
                continue;
            }

            LivingEntity target = evoker.getTarget();
            if (target == null
                    || target.isDead()
                    || !target.isValid()) {
                behavior.clearTrapRiderTarget(wolf);
                continue;
            }

            behavior.setTrapRiderTarget(wolf, target);
            wolf.getPathfinder().moveTo(target, 1.2);
        }
    }

    private void activate(Evoker initial) {
        if (!states.isArmed(initial)) {
            return;
        }

        Location origin = initial.getLocation();
        strikeOwnLightning(origin);

        WonderfulWolfEntityCreationResult firstResult =
                wolves.spawnFounder(
                        origin,
                        FounderOrigin.WOLF_TRAP,
                        seeds.getAsLong());
        WonderfulWolfEntityCreationResult secondResult =
                wolves.spawnFounder(
                        origin.clone().add(1.0, 0.0, 0.0),
                        FounderOrigin.WOLF_TRAP,
                        seeds.getAsLong());

        Wolf first = createdWolf(firstResult);
        Wolf second = createdWolf(secondResult);
        if (first == null || second == null) {
            if (first != null) first.remove();
            if (second != null) second.remove();
            logger.warning(
                    "Could not create both Wonderful Wolf trap founders");
            return;
        }

        Evoker secondRider = initial.getWorld().spawn(
                origin.clone().add(1.0, 0.0, 0.0),
                Evoker.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM);

        armedEvokers.remove(initial.getUniqueId());
        states.markRider(initial);
        states.markRider(secondRider);
        states.markMountedWolf(first);
        states.markMountedWolf(second);
        first.addPassenger(initial);
        second.addPassenger(secondRider);
        riderEvokers.add(initial.getUniqueId());
        riderEvokers.add(secondRider.getUniqueId());
    }

    private Wolf createdWolf(
            WonderfulWolfEntityCreationResult result) {
        if (result instanceof WonderfulWolfEntityCreationResult.Success success) {
            return success.wolf();
        }
        if (result instanceof WonderfulWolfEntityCreationResult.AlreadyWonderful existing) {
            return existing.wolf();
        }
        return null;
    }

    private void spawnArmed(
            Location location,
            long graceUntilMillis) {
        World world = Objects.requireNonNull(
                location.getWorld(),
                "location world");
        Evoker evoker = world.spawn(
                location,
                Evoker.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM);
        states.markArmed(evoker, graceUntilMillis);
        armedEvokers.add(evoker.getUniqueId());
    }

    private Location randomSkyLocation(Player player) {
        World world = player.getWorld();
        WolfTrapPolicy.Offset offset = WolfTrapPolicy.offset(
                draw(),
                draw(),
                WolfTrapPolicy.ADDITIONAL_RADIUS_BLOCKS);
        int x = (int) Math.floor(player.getX() + offset.x());
        int z = (int) Math.floor(player.getZ() + offset.z());
        int y = world.getHighestBlockYAt(
                x,
                z,
                HeightMap.MOTION_BLOCKING);
        return new Location(
                world,
                x + 0.5,
                Math.min(world.getMaxHeight() - 1, y + 1),
                z + 0.5);
    }

    private void strikeOwnLightning(Location location) {
        creatingOwnLightning = true;
        try {
            Objects.requireNonNull(
                    location.getWorld(),
                    "location world")
                    .strikeLightning(location);
        } finally {
            creatingOwnLightning = false;
        }
    }

    private void restoreMountedWolf(Wolf wolf) {
        if (!states.isMountedWolf(wolf)) {
            return;
        }
        boolean hasRider = wolf.getPassengers().stream()
                .filter(Evoker.class::isInstance)
                .map(Evoker.class::cast)
                .anyMatch(states::isRider);
        if (!hasRider) {
            states.clearMountedWolf(wolf);
            behavior.clearTrapRiderTarget(wolf);
        }
    }

    private double draw() {
        double value = random.getAsDouble();
        if (!Double.isFinite(value)
                || value < 0.0
                || value >= 1.0) {
            throw new IllegalStateException(
                    "random supplier must return [0, 1)");
        }
        return value;
    }
}
