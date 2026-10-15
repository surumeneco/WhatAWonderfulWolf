package co.surumene.www.behavior;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.individual.WorldPosition;
import co.surumene.www.individual.WonderfulWolfIndividual;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class CommandStatePolicyTest {
    private static final UUID COMMANDER =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final WorldPosition WAIT =
            new WorldPosition(
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                    12.5, 64.0, -8.0);

    @Test
    void modeChangesAreNeverRejectedByAffection() {
        WonderfulWolfIndividual wolf =
                BehaviorTestIndividuals.individual(0, 10);
        wolf = AffectionPolicy.adjust(wolf, COMMANDER, -999999L);

        WonderfulWolfIndividual follow =
                CommandStatePolicy.changeMode(
                        wolf,
                        Mode.FOLLOW,
                        COMMANDER,
                        WAIT);
        assertEquals(Mode.FOLLOW, follow.mode());
        assertEquals(Optional.of(COMMANDER), follow.commanderId());
    }

    @Test
    void wanderClearsCommanderAndWaitCapturesTheCurrentLocation() {
        WonderfulWolfIndividual wolf =
                BehaviorTestIndividuals.individual(0, 10);

        WonderfulWolfIndividual wait =
                CommandStatePolicy.changeMode(
                        wolf,
                        Mode.WAIT,
                        COMMANDER,
                        WAIT);
        assertEquals(Optional.of(COMMANDER), wait.commanderId());
        assertEquals(Optional.of(WAIT), wait.waitLocation());

        WonderfulWolfIndividual wander =
                CommandStatePolicy.changeMode(
                        wait,
                        Mode.WANDER,
                        COMMANDER,
                        new WorldPosition(WAIT.worldId(), 99, 99, 99));
        assertTrue(wander.commanderId().isEmpty());
        assertEquals(Optional.of(WAIT), wander.waitLocation());
    }

    @Test
    void actionDistanceChangesIndependentlyOfModeAndAffection() {
        WonderfulWolfIndividual wolf =
                BehaviorTestIndividuals.individual(0, 10);
        WonderfulWolfIndividual changed =
                CommandStatePolicy.changeActionDistance(
                        wolf,
                        ActionDistance.VERY_WIDE);
        assertEquals(ActionDistance.VERY_WIDE, changed.actionDistance());
        assertEquals(Mode.WANDER, changed.mode());
    }
}
