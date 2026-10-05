package co.surumene.www.ability;

import co.surumene.www.domain.Ability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AbilityScaleTest {
    @Test
    void convertsNormalizedValuesToCanonicalRangesAndExtendsAboveOne() {
        assertEquals(20.0, AbilityScale.toCanonical(Ability.HEALTH, 0.0));
        assertEquals(60.0, AbilityScale.toCanonical(Ability.HEALTH, 1.0));
        assertEquals(80.0, AbilityScale.toCanonical(Ability.HEALTH, 1.5));

        assertEquals(1.5, AbilityScale.toCanonical(Ability.SIZE, 0.0));
        assertEquals(3.0, AbilityScale.toCanonical(Ability.SIZE, 1.0));
        assertEquals(3.75, AbilityScale.toCanonical(Ability.SIZE, 1.5));

        assertEquals(3.0, AbilityScale.toCanonical(Ability.MOVEMENT_SPEED, 0.0));
        assertEquals(24.0, AbilityScale.toCanonical(Ability.MOVEMENT_SPEED, 1.0));
        assertEquals(34.5, AbilityScale.toCanonical(Ability.MOVEMENT_SPEED, 1.5));

        assertEquals(1.0, AbilityScale.toCanonical(Ability.JUMP, 0.0));
        assertEquals(5.0, AbilityScale.toCanonical(Ability.JUMP, 1.0));
        assertEquals(0.5, AbilityScale.toCanonical(Ability.STEP_HEIGHT, 0.0));
        assertEquals(1.5, AbilityScale.toCanonical(Ability.STEP_HEIGHT, 1.0));

        assertEquals(1.0, AbilityScale.toCanonical(Ability.ATTACK_DAMAGE, 0.0));
        assertEquals(10.0, AbilityScale.toCanonical(Ability.ATTACK_DAMAGE, 1.0));
        assertEquals(0.2, AbilityScale.toCanonical(Ability.ATTACK_SPEED, 0.0));
        assertEquals(2.0, AbilityScale.toCanonical(Ability.ATTACK_SPEED, 1.0));

        assertEquals(0.0, AbilityScale.toCanonical(Ability.DEFENSE, 0.0));
        assertEquals(30.0, AbilityScale.toCanonical(Ability.DEFENSE, 1.0));
        assertEquals(0.75, AbilityScale.toCanonical(Ability.PATIENCE, 0.75));
    }

    @Test
    void canonicalDiscreteAbilitiesUseNearestIntegerAndNormalInventoryCapsAt45() {
        assertEquals(40.0, AbilityScale.toCanonical(Ability.HEALTH, 0.5));
        assertEquals(23.0, AbilityScale.toCanonical(Ability.INVENTORY, 0.5));
        assertEquals(68.0, AbilityScale.toCanonical(Ability.INVENTORY, 1.5));
        assertEquals(45.0, AbilityScale.finalizeEffective(Ability.INVENTORY, 67.5));
    }

    @Test
    void ranksFollowWwcBoundaries() {
        assertEquals(AbilityRank.MISERABLE, AbilityRank.fromNormalized(0.0));
        assertEquals(AbilityRank.MISERABLE, AbilityRank.fromNormalized(Math.nextDown(1.0 / 9.0)));
        assertEquals(AbilityRank.VERY_LOW, AbilityRank.fromNormalized(1.0 / 9.0));
        assertEquals(AbilityRank.LEGENDARY, AbilityRank.fromNormalized(1.0));
        assertEquals(AbilityRank.MYTHICAL, AbilityRank.fromNormalized(Math.nextUp(1.0)));
        assertEquals(AbilityRank.MYTHICAL, AbilityRank.fromNormalized(1.25));
        assertEquals(AbilityRank.IMPOSSIBLE, AbilityRank.fromNormalized(Math.nextUp(1.25)));
    }
}
