package co.surumene.www.spawn;

import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.wgl.api.DiploidGenome;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

public final class PaperWonderfulWolfFactory implements NaturalWolfConverter {
    private final FounderIndividualSource founderSource;
    private final WonderfulWolfLoadedIndividuals loaded;
    private final LongSupplier biologicalTime;
    private final Consumer<Wolf> abilityRefresh;
    private final Logger logger;

    public PaperWonderfulWolfFactory(
            FounderIndividualSource founderSource,
            WonderfulWolfLoadedIndividuals loaded,
            LongSupplier biologicalTime,
            Consumer<Wolf> abilityRefresh) {
        this(
                founderSource,
                loaded,
                biologicalTime,
                abilityRefresh,
                Logger.getLogger(PaperWonderfulWolfFactory.class.getName()));
    }

    public PaperWonderfulWolfFactory(
            FounderIndividualSource founderSource,
            WonderfulWolfLoadedIndividuals loaded,
            LongSupplier biologicalTime,
            Consumer<Wolf> abilityRefresh,
            Logger logger) {
        this.founderSource = Objects.requireNonNull(founderSource, "founderSource");
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.biologicalTime = Objects.requireNonNull(biologicalTime, "biologicalTime");
        this.abilityRefresh = Objects.requireNonNull(abilityRefresh, "abilityRefresh");
        this.logger = Objects.requireNonNull(logger, "logger");
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

        if (wolf.isAdult()) {
            try {
                abilityRefresh.accept(wolf);
            } catch (RuntimeException error) {
                logger.warning(
                        "Could not immediately project Wonderful Wolf attributes for "
                                + wolf.getUniqueId()
                                + ": "
                                + safeMessage(error));
            }
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
            abilityRefresh.accept(wolf);
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
