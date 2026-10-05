package co.surumene.www.spawn;

import co.surumene.www.individual.WonderfulWolfIndividual;
import org.bukkit.entity.Wolf;

import java.util.Objects;

public sealed interface WonderfulWolfEntityCreationResult
        permits WonderfulWolfEntityCreationResult.Success,
        WonderfulWolfEntityCreationResult.AlreadyWonderful,
        WonderfulWolfEntityCreationResult.Failure {

    record Success(
            Wolf wolf,
            WonderfulWolfIndividual individual)
            implements WonderfulWolfEntityCreationResult {
        public Success {
            Objects.requireNonNull(wolf, "wolf");
            Objects.requireNonNull(individual, "individual");
        }
    }

    record AlreadyWonderful(
            Wolf wolf,
            WonderfulWolfIndividual individual)
            implements WonderfulWolfEntityCreationResult {
        public AlreadyWonderful {
            Objects.requireNonNull(wolf, "wolf");
            Objects.requireNonNull(individual, "individual");
        }
    }

    record Failure(
            Wolf wolf,
            String reason,
            String detail)
            implements WonderfulWolfEntityCreationResult {
        public Failure {
            Objects.requireNonNull(wolf, "wolf");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }
}
