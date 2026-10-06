package co.surumene.www.behavior;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.individual.WorldPosition;
import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CommandStatePolicy {
    private CommandStatePolicy() {}

    public static WonderfulWolfIndividual changeMode(
            WonderfulWolfIndividual individual,
            Mode mode,
            UUID actorId,
            WorldPosition currentPosition) {
        Objects.requireNonNull(individual, "individual");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(currentPosition, "currentPosition");

        Optional<UUID> commander =
                mode == Mode.WANDER
                        ? Optional.empty()
                        : Optional.of(actorId);
        Optional<WorldPosition> waitLocation =
                mode == Mode.WAIT
                        ? Optional.of(currentPosition)
                        : individual.waitLocation();

        return individual.withCommandState(
                mode,
                commander,
                individual.actionDistance(),
                waitLocation);
    }

    public static WonderfulWolfIndividual changeActionDistance(
            WonderfulWolfIndividual individual,
            ActionDistance actionDistance) {
        Objects.requireNonNull(individual, "individual");
        Objects.requireNonNull(actionDistance, "actionDistance");
        return individual.withCommandState(
                individual.mode(),
                individual.commanderId(),
                actionDistance,
                individual.waitLocation());
    }
}
