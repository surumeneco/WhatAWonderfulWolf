package co.surumene.www.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression contract for the WCL decision delegated to by the WWW
 * status-effect adapter. No Paper potion registry is needed by this test.
 */
class WonderfulWolfStatusEffectInfiniteTest {
    @Test
    void preservesExistingInfiniteSameLevelEffect() {
        assertFalse(EffectPolicy.shouldApplyPotion(0, -1, 0, 40));
    }

    @Test
    void preservesStrongerInfiniteEffect() {
        assertFalse(EffectPolicy.shouldApplyPotion(1, -1, 0, 40));
    }

    @Test
    void stillAllowsHigherLevelToReplaceWeakerInfiniteEffect() {
        assertTrue(EffectPolicy.shouldApplyPotion(0, -1, 1, 40));
    }
}
