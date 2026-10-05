package co.surumene.www.spawn;

import co.surumene.www.founder.FounderOrigin;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface FounderIndividualSource {
    WonderfulWolfCreationResult createFounder(
            FounderOrigin origin,
            Optional<UUID> ownerId,
            long adultBiologicalTime,
            long seed);
}
