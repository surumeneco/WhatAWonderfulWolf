package co.surumene.www.behavior;

import co.surumene.www.individual.WonderfulWolfIndividual;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class AffectionPolicyTest {
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void missingPlayerStartsFromPhenotypeInitialAffection() {
        WonderfulWolfIndividual wolf = BehaviorTestIndividuals.individual(-25, 12);
        assertEquals(-25L, AffectionPolicy.current(wolf, A));
    }

    @Test
    void adjustmentsClampAtTheAbsoluteSupportedRangeWithoutOverflow() {
        WonderfulWolfIndividual wolf = BehaviorTestIndividuals.individual(0, 15);

        wolf = AffectionPolicy.adjust(
                wolf,
                A,
                WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION);
        wolf = AffectionPolicy.adjust(wolf, A, Long.MAX_VALUE);
        assertEquals(
                WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION,
                AffectionPolicy.current(wolf, A));

        wolf = AffectionPolicy.adjust(
                wolf,
                B,
                -WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION);
        wolf = AffectionPolicy.adjust(wolf, B, Long.MIN_VALUE);
        assertEquals(
                -WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION,
                AffectionPolicy.current(wolf, B));
    }

    @Test
    void feedAndDamageUseTheIndividualRelationshipDelta() {
        WonderfulWolfIndividual wolf = BehaviorTestIndividuals.individual(10, 7);
        wolf = AffectionPolicy.reward(wolf, A);
        assertEquals(17L, AffectionPolicy.current(wolf, A));
        wolf = AffectionPolicy.penalize(wolf, A);
        assertEquals(10L, AffectionPolicy.current(wolf, A));
        assertEquals(Map.of(A, 10L), wolf.affection());
    }
}
