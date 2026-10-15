package co.surumene.www.spawn;

import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.wgl.api.DiploidGenome;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

public final class PaperWonderfulWolfFactory implements NaturalWolfConverter {
    private final FounderIndividualSource founderSource;
    private final WonderfulWolfLoadedIndividuals loaded;
    private final LongSupplier biologicalTime;
    private final Consumer<Wolf> initializeNew;
    private final Logger logger;
    private AsyncWolfFounderCoordinator async;
    private NamespacedKey deferredSeedKey;
    private final Map<UUID, Wolf> pendingNatural = new HashMap<>();

    public PaperWonderfulWolfFactory(
            FounderIndividualSource founderSource,
            WonderfulWolfLoadedIndividuals loaded,
            LongSupplier biologicalTime,
            Consumer<Wolf> initializeNew) {
        this(
                founderSource,
                loaded,
                biologicalTime,
                initializeNew,
                Logger.getLogger(PaperWonderfulWolfFactory.class.getName()));
    }

    public PaperWonderfulWolfFactory(
            FounderIndividualSource founderSource,
            WonderfulWolfLoadedIndividuals loaded,
            LongSupplier biologicalTime,
            Consumer<Wolf> initializeNew,
            Logger logger) {
        this.founderSource = Objects.requireNonNull(founderSource, "founderSource");
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.biologicalTime = Objects.requireNonNull(biologicalTime, "biologicalTime");
        this.initializeNew = Objects.requireNonNull(initializeNew, "initializeNew");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.loaded.onFirstCreation(wolf -> {
            try {
                initializeNew.accept(wolf);
            } catch (RuntimeException error) {
                logger.warning("Could not initialize new WWW wolf "
                        + wolf.getUniqueId() + ": " + safeMessage(error));
            }
        });
    }

    public void useAsyncCoordinator(AsyncWolfFounderCoordinator coordinator) {
        this.async = Objects.requireNonNull(coordinator);
        deferredSeedKey = new NamespacedKey(coordinator.plugin(), "pending_natural_founder_seed");
    }

    /** Discard stale jobs bound to a removed entity instance, preserving its PDC retry seed. */
    public void invalidateNatural(Wolf wolf) {
        pendingNatural.remove(wolf.getUniqueId(), wolf);
    }

    public void resumeAfterEntityAddition(Wolf wolf) {
        if (async == null) return;
        org.bukkit.Bukkit.getScheduler().runTaskLater(async.plugin(), () -> {
            if (wolf.isValid() && !wolf.isDead()) resumePendingNatural(wolf);
        }, 1L);
    }

    public boolean resumePendingNatural(Wolf wolf) {
        if (deferredSeedKey == null) return false;
        Long seed = wolf.getPersistentDataContainer().get(deferredSeedKey, PersistentDataType.LONG);
        if (seed == null) return false;
        requestNaturalAsync(wolf, seed);
        return true;
    }

    /** Only WGL calculation runs in the worker; all entity/PDC access is synchronous. */
    public boolean requestNaturalAsync(Wolf wolf, long seed) {
        if (async == null || !(founderSource instanceof WonderfulWolfFactory factory)) {
            convert(wolf, seed);
            return true;
        }
        if (wolf.isDead()) return false;
        UUID id = wolf.getUniqueId();
        Wolf current = pendingNatural.get(id);
        if (current == wolf) return true;
        // A ClickMobs replacement can have the same UUID but a new Bukkit wrapper.
        if (current != null) pendingNatural.remove(id, current);
        if (loaded.find(id).isPresent()) {
            wolf.getPersistentDataContainer().remove(deferredSeedKey);
            return true;
        }
        Long previous = wolf.getPersistentDataContainer().get(deferredSeedKey, PersistentDataType.LONG);
        long selectedSeed = previous == null ? seed : previous;
        if (previous == null) wolf.getPersistentDataContainer().set(
                deferredSeedKey, PersistentDataType.LONG, selectedSeed);
        Optional<UUID> owner = wolf.isTamed()
                ? Optional.ofNullable(wolf.getOwner()).map(AnimalTamer::getUniqueId)
                : Optional.empty();
        boolean adult = wolf.isAdult();
        long adultTime = adult ? Math.max(0L, biologicalTime.getAsLong()) : 0L;
        WonderfulWolfFactory frozen = factory.snapshot();
        pendingNatural.put(id, wolf);
        boolean accepted = async.submit(
                () -> frozen.createFounder(FounderOrigin.NATURAL, owner, adultTime, selectedSeed),
                result -> {
                    if (!pendingNatural.remove(id, wolf) || !wolf.isValid() || wolf.isDead()) return;
                    Optional<UUID> currentOwner = wolf.isTamed()
                            ? Optional.ofNullable(wolf.getOwner()).map(AnimalTamer::getUniqueId)
                            : Optional.empty();
                    if (!owner.equals(currentOwner) || adult != wolf.isAdult()) {
                        // Taming/growth during synthesis must not commit stale identity data.
                        requestNaturalAsync(wolf, selectedSeed);
                        return;
                    }
                    RestoreResult state = loaded.register(wolf);
                    if (!(state instanceof RestoreResult.NotWonderful)) return;
                    if (result instanceof WonderfulWolfCreationResult.Success success) {
                        try {
                            loaded.saveAndRegister(wolf, success.individual());
                            wolf.getPersistentDataContainer().remove(deferredSeedKey);
                        } catch (RuntimeException error) {
                            logger.warning("WWW natural founder apply failed for " + id + ": " + error);
                        }
                    } else if (result instanceof WonderfulWolfCreationResult.Failure failure) {
                        logger.warning("WWW natural founder failed for " + id + ": " + failure.detail());
                    }
                }, error -> {
                    pendingNatural.remove(id, wolf);
                    logger.warning("WWW natural founder calculation failed for " + id + ": " + error);
                });
        if (!accepted) pendingNatural.remove(id, wolf);
        return accepted;
    }

    /** Asynchronous command founder computation without creating a placeholder Wolf. */
    public boolean prepareFounderAsync(
            FounderOrigin origin, long seed,
            Consumer<WonderfulWolfCreationResult> callback) {
        if (async == null || !(founderSource instanceof WonderfulWolfFactory factory)) return false;
        WonderfulWolfFactory frozen = factory.snapshot();
        long adultTime = Math.max(0L, biologicalTime.getAsLong());
        return async.submit(
                () -> frozen.createFounder(origin, Optional.empty(), adultTime, seed),
                callback,
                error -> callback.accept(
                        new WonderfulWolfCreationResult.Failure("SYNTHESIS_FAILED", error.toString())));
    }

    public record PreparedPair(WonderfulWolfCreationResult first, WonderfulWolfCreationResult second) {}

    /** Calculate both trap animals before spawning either one. */
    public boolean prepareTrapPairAsync(long firstSeed, long secondSeed,
                                         Consumer<PreparedPair> callback) {
        if (async == null || !(founderSource instanceof WonderfulWolfFactory factory)) return false;
        WonderfulWolfFactory frozen = factory.snapshot();
        long adultTime = Math.max(0L, biologicalTime.getAsLong());
        return async.submit(() -> new PreparedPair(
                        frozen.createFounder(FounderOrigin.WOLF_TRAP, Optional.empty(), adultTime, firstSeed),
                        frozen.createFounder(FounderOrigin.WOLF_TRAP, Optional.empty(), adultTime, secondSeed)),
                callback,
                error -> {
                    logger.warning("WWW trap founder calculation failed: " + error);
                    callback.accept(new PreparedPair(
                            new WonderfulWolfCreationResult.Failure("SYNTHESIS_FAILED", error.toString()),
                            new WonderfulWolfCreationResult.Failure("SYNTHESIS_FAILED", error.toString())));
                });
    }

    public WonderfulWolfEntityCreationResult spawnPreparedFounder(
            Location location, WonderfulWolfIndividual individual) {
        Objects.requireNonNull(individual);
        World world = Objects.requireNonNull(location.getWorld());
        Wolf wolf = world.spawn(location, Wolf.class, CreatureSpawnEvent.SpawnReason.CUSTOM);
        try {
            loaded.saveAndRegister(wolf, individual);
            return new WonderfulWolfEntityCreationResult.Success(wolf, individual);
        } catch (RuntimeException error) {
            loaded.unregister(wolf);
            wolf.remove();
            return new WonderfulWolfEntityCreationResult.Failure(wolf, "PERSISTENCE_FAILED", safeMessage(error));
        }
    }

    @Override
    public WonderfulWolfEntityCreationResult convert(Wolf wolf, long seed) {
        return convertExisting(wolf, FounderOrigin.NATURAL, seed);
    }

    public WonderfulWolfEntityCreationResult convertExisting(
            Wolf wolf,
            FounderOrigin origin,
            long seed) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(origin, "origin");

        RestoreResult existing = loaded.register(wolf);
        if (existing instanceof RestoreResult.Success success) {
            return new WonderfulWolfEntityCreationResult.AlreadyWonderful(
                    wolf,
                    success.individual());
        }
        if (existing instanceof RestoreResult.Failure failure) {
            return new WonderfulWolfEntityCreationResult.Failure(
                    wolf,
                    "PERSISTED_STATE_" + failure.reason().name(),
                    failure.detail());
        }

        Optional<UUID> ownerId = Optional.empty();
        if (wolf.isTamed()) {
            ownerId = Optional.ofNullable(wolf.getOwner())
                    .map(AnimalTamer::getUniqueId);
        }
        long adultTime = wolf.isAdult()
                ? Math.max(0L, biologicalTime.getAsLong())
                : 0L;

        WonderfulWolfCreationResult created =
                founderSource.createFounder(
                        origin,
                        ownerId,
                        adultTime,
                        seed);
        if (created instanceof WonderfulWolfCreationResult.Failure failure) {
            return new WonderfulWolfEntityCreationResult.Failure(
                    wolf,
                    failure.reason(),
                    failure.detail());
        }

        WonderfulWolfIndividual individual =
                ((WonderfulWolfCreationResult.Success) created).individual();
        try {
            loaded.saveAndRegister(wolf, individual);
        } catch (RuntimeException error) {
            return new WonderfulWolfEntityCreationResult.Failure(
                    wolf,
                    "PERSISTENCE_FAILED",
                    safeMessage(error));
        }


        return new WonderfulWolfEntityCreationResult.Success(wolf, individual);
    }

    public WonderfulWolfEntityCreationResult spawnGenome(
            Location location,
            DiploidGenome genome) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(genome, "genome");
        World world = Objects.requireNonNull(
                location.getWorld(),
                "location world");

        Wolf wolf = world.spawn(
                location,
                Wolf.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM);
        WonderfulWolfCreationResult created =
                founderSource.createFromGenome(
                        genome,
                        Optional.empty(),
                        Math.max(0L, biologicalTime.getAsLong()));
        if (created instanceof WonderfulWolfCreationResult.Failure failure) {
            wolf.remove();
            return new WonderfulWolfEntityCreationResult.Failure(
                    wolf,
                    failure.reason(),
                    failure.detail());
        }

        WonderfulWolfIndividual individual =
                ((WonderfulWolfCreationResult.Success) created).individual();
        try {
            loaded.saveAndRegister(wolf, individual);
        } catch (RuntimeException error) {
            loaded.unregister(wolf);
            wolf.remove();
            return new WonderfulWolfEntityCreationResult.Failure(
                    wolf,
                    "PERSISTENCE_FAILED",
                    safeMessage(error));
        }
        return new WonderfulWolfEntityCreationResult.Success(
                wolf,
                individual);
    }

    public WonderfulWolfEntityCreationResult spawnFounder(
            Location location,
            FounderOrigin origin,
            long seed) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(origin, "origin");
        World world = Objects.requireNonNull(
                location.getWorld(),
                "location world");

        Wolf wolf = world.spawn(
                location,
                Wolf.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM);

        WonderfulWolfEntityCreationResult result =
                convertExisting(wolf, origin, seed);
        if (result instanceof WonderfulWolfEntityCreationResult.Failure) {
            wolf.remove();
        }
        return result;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
