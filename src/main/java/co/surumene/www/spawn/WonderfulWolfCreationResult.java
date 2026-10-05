package co.surumene.www.spawn;

import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.Objects;

public sealed interface WonderfulWolfCreationResult
        permits WonderfulWolfCreationResult.Success,
        WonderfulWolfCreationResult.Failure {

    record Success(WonderfulWolfIndividual individual)
            implements WonderfulWolfCreationResult {
        public Success {
            Objects.requireNonNull(individual, "individual");
        }
    }

    record Failure(String reason, String detail)
            implements WonderfulWolfCreationResult {
        public Failure {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }
}
