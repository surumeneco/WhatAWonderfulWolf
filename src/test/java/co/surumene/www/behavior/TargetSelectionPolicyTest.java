package co.surumene.www.behavior;

import co.surumene.www.domain.Mode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class TargetSelectionPolicyTest {
    private static final UUID MANUAL = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SELF = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID COMMAND = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PVP = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID ACTIVE = UUID.fromString("50000000-0000-0000-0000-000000000005");

    @Test
    void retreatOverridesAllTargetSelection() {
        assertTrue(TargetSelectionPolicy.select(
                Mode.GUARD,
                true,
                List.of(candidate(MANUAL, TargetSource.MANUAL, true, 1, 1)))
                .isEmpty());
    }

    @Test
    void wanderOnlyUsesSelfDefenseAndNeverManualOrActiveSearch() {
        List<TargetCandidate> candidates = List.of(
                candidate(MANUAL, TargetSource.MANUAL, true, 1, 1),
                candidate(ACTIVE, TargetSource.ACTIVE_SEARCH, true, 1, 1),
                candidate(SELF, TargetSource.SELF_ATTACKER, false, 4, 100));

        assertEquals(
                SELF,
                TargetSelectionPolicy.select(Mode.WANDER, false, candidates)
                        .orElseThrow().targetId());
    }

    @Test
    void followDoesNotActivelySearchAndManualBeatsReactiveTargets() {
        List<TargetCandidate> candidates = List.of(
                candidate(ACTIVE, TargetSource.ACTIVE_SEARCH, true, 1, 1),
                candidate(SELF, TargetSource.SELF_ATTACKER, true, 1, 1),
                candidate(COMMAND, TargetSource.COMMAND_COMBAT, true, 1, 1),
                candidate(MANUAL, TargetSource.MANUAL, true, 100, 100));

        assertEquals(
                MANUAL,
                TargetSelectionPolicy.select(Mode.FOLLOW, false, candidates)
                        .orElseThrow().targetId());
    }

    @Test
    void guardAndWaitCanUseActiveSearchButRespectActionDistance() {
        List<TargetCandidate> candidates = List.of(
                candidate(ACTIVE, TargetSource.ACTIVE_SEARCH, true, 10, 10),
                candidate(SELF, TargetSource.SELF_ATTACKER, false, 1, 1));

        assertEquals(
                ACTIVE,
                TargetSelectionPolicy.select(Mode.GUARD, false, candidates)
                        .orElseThrow().targetId());
        assertEquals(
                ACTIVE,
                TargetSelectionPolicy.select(Mode.WAIT, false, candidates)
                        .orElseThrow().targetId());
    }

    @Test
    void samePriorityUsesWolfDistanceForFollowAndReferenceDistanceForGuardAndWait() {
        TargetCandidate a = candidate(SELF, TargetSource.SELF_ATTACKER, true, 4, 25);
        TargetCandidate b = candidate(COMMAND, TargetSource.SELF_ATTACKER, true, 9, 1);

        assertEquals(
                SELF,
                TargetSelectionPolicy.select(Mode.FOLLOW, false, List.of(a, b))
                        .orElseThrow().targetId());
        assertEquals(
                COMMAND,
                TargetSelectionPolicy.select(Mode.GUARD, false, List.of(a, b))
                        .orElseThrow().targetId());
        assertEquals(
                COMMAND,
                TargetSelectionPolicy.select(Mode.WAIT, false, List.of(a, b))
                        .orElseThrow().targetId());
    }

    @Test
    void pvpInterventionRanksBelowSelfAndCommandCombatButAboveActiveSearch() {
        List<TargetCandidate> candidates = List.of(
                candidate(ACTIVE, TargetSource.ACTIVE_SEARCH, true, 1, 1),
                candidate(PVP, TargetSource.PVP_INTERVENTION, true, 1, 1),
                candidate(COMMAND, TargetSource.COMMAND_COMBAT, true, 1, 1));

        assertEquals(
                COMMAND,
                TargetSelectionPolicy.select(Mode.GUARD, false, candidates)
                        .orElseThrow().targetId());
    }

    private static TargetCandidate candidate(
            UUID id,
            TargetSource source,
            boolean withinDistance,
            double wolfDistanceSquared,
            double referenceDistanceSquared) {
        return new TargetCandidate(
                id,
                source,
                withinDistance,
                wolfDistanceSquared,
                referenceDistanceSquared);
    }
}
