package co.surumene.www.persistence;

import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.Objects;

public sealed interface RestoreResult
        permits RestoreResult.NotWonderful, RestoreResult.Success, RestoreResult.Failure {

    record NotWonderful() implements RestoreResult {}

    record Success(WonderfulWolfIndividual individual) implements RestoreResult {
        public Success {
            Objects.requireNonNull(individual, "individual");
        }
    }

    record Failure(RestoreFailureReason reason, String detail) implements RestoreResult {
        public Failure {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
        }
    }
}
