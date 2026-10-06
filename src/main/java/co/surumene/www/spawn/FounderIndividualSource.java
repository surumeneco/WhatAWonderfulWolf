package co.surumene.www.spawn;

import co.surumene.www.founder.FounderOrigin;
import co.surumene.wgl.api.DiploidGenome;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface FounderIndividualSource {
    WonderfulWolfCreationResult createFounder(
            FounderOrigin origin,
            Optional<UUID> ownerId,
            long adultBiologicalTime,
            long seed);

    default WonderfulWolfCreationResult createFromGenome(
            DiploidGenome genome,
            Optional<UUID> ownerId,
            long adultBiologicalTime) {
        throw new UnsupportedOperationException(
                "arbitrary genome creation is not supported");
    }
}
