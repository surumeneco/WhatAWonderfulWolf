package co.surumene.www.breeding;

import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

public final class WonderfulWolfBreedingListener implements Listener {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfBreedingService breeding;
    private final LongSupplier seedSupplier;
    private final Logger logger;

    public WonderfulWolfBreedingListener(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfBreedingService breeding,
            LongSupplier seedSupplier,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.breeding = Objects.requireNonNull(breeding, "breeding");
        this.seedSupplier = Objects.requireNonNull(seedSupplier, "seedSupplier");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Wolf child)
                || !(event.getMother() instanceof Wolf mother)
                || !(event.getFather() instanceof Wolf father)) {
            return;
        }

        Optional<WonderfulWolfIndividual> motherState = resolve(mother);
        Optional<WonderfulWolfIndividual> fatherState = resolve(father);
        if (motherState.isEmpty() || fatherState.isEmpty()) {
            return;
        }

        Optional<UUID> childOwner = Optional.ofNullable(child.getOwner())
                .map(AnimalTamer::getUniqueId);

        WonderfulWolfBreedingOutcome outcome;
        try {
            outcome = breeding.breed(
                    new BreedingParent(displayName(mother), motherState.orElseThrow()),
                    new BreedingParent(displayName(father), fatherState.orElseThrow()),
                    childOwner,
                    seedSupplier.getAsLong());
        } catch (RuntimeException error) {
            logger.warning(
                    "WWW breeding failed unexpectedly for child "
                            + child.getUniqueId()
                            + ": "
                            + safeMessage(error));
            return;
        }

        if (outcome instanceof WonderfulWolfBreedingOutcome.Success success) {
            loaded.saveAndRegister(child, success.child());
        }
    }

    private Optional<WonderfulWolfIndividual> resolve(Wolf wolf) {
        Optional<WonderfulWolfIndividual> cached = loaded.find(wolf.getUniqueId());
        if (cached.isPresent()) {
            return cached;
        }

        RestoreResult restored = loaded.register(wolf);
        if (restored instanceof RestoreResult.Success success) {
            return Optional.of(success.individual());
        }
        if (restored instanceof RestoreResult.Failure failure) {
            logger.warning(
                    "Failed to restore WWW breeding parent "
                            + wolf.getUniqueId()
                            + " ("
                            + failure.reason()
                            + "): "
                            + failure.detail());
        }
        return Optional.empty();
    }

    private static String displayName(Wolf wolf) {
        Component custom = wolf.customName();
        if (custom != null) {
            String text = PlainTextComponentSerializer.plainText().serialize(custom);
            if (!text.isBlank()) {
                return text;
            }
        }
        return wolf.getName();
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
