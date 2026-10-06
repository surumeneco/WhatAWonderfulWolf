package co.surumene.www.behavior;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class ManualTargetRegistryTest {
    private static final UUID WOLF =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID A =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID B =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID TARGET_A =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID TARGET_B =
            UUID.fromString("40000000-0000-0000-0000-000000000004");

    @Test
    void sameIssuerCanReplaceItsInstructionWithoutCompetition() {
        ManualTargetRegistry registry = new ManualTargetRegistry();
        var individual = BehaviorTestIndividuals.individual(0, 10);

        assertTrue(registry.submit(
                individual, WOLF, A, TARGET_A, 0.99));
        assertTrue(registry.submit(
                individual, WOLF, A, TARGET_B, 0.99));
        assertEquals(
                TARGET_B,
                registry.get(WOLF).orElseThrow().targetId());
    }

    @Test
    void aCompetingIssuerOnlyReplacesTheExistingInstructionIfItWins() {
        ManualTargetRegistry registry = new ManualTargetRegistry();
        var individual = BehaviorTestIndividuals.individual(0, 10);
        individual = AffectionPolicy.adjust(individual, A, 10L);
        individual = AffectionPolicy.adjust(individual, B, 20L);

        assertTrue(registry.submit(
                individual, WOLF, A, TARGET_A, 0.0));
        assertFalse(registry.submit(
                individual, WOLF, B, TARGET_B, 0.20));
        assertEquals(
                TARGET_A,
                registry.get(WOLF).orElseThrow().targetId());

        assertTrue(registry.submit(
                individual, WOLF, B, TARGET_B, 0.90));
        assertEquals(
                B,
                registry.get(WOLF).orElseThrow().issuerId());
        assertEquals(
                TARGET_B,
                registry.get(WOLF).orElseThrow().targetId());
    }
}
