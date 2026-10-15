package co.surumene.www.runtime;

import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class EffectPolicyTest {
    @Test
    void amplifierUsesStrongOnlyForLevelledEffects() {
        assertEquals(0, EffectPolicy.amplifier(Trait.UNYIELDING, TraitStrength.WEAK));
        assertEquals(1, EffectPolicy.amplifier(Trait.UNYIELDING, TraitStrength.STRONG));
        assertEquals(0, EffectPolicy.amplifier(Trait.FIRE_BIRD, TraitStrength.STRONG));
        assertEquals(0, EffectPolicy.amplifier(Trait.SWIMMER, TraitStrength.STRONG));
    }

    @Test
    void damageProtectionMatchesSpecification() {
        assertTrue(EffectPolicy.immuneTo(Trait.LIGHTWEIGHT, EffectPolicy.DamageKind.FALL));
        assertTrue(EffectPolicy.immuneTo(Trait.SWIMMER, EffectPolicy.DamageKind.DROWNING));
        assertTrue(EffectPolicy.immuneTo(Trait.SNOWBORN, EffectPolicy.DamageKind.FREEZING));
        assertFalse(EffectPolicy.immuneTo(Trait.FIRE_BIRD, EffectPolicy.DamageKind.FALL));
    }

    @Test
    void watchmanIntervalIsLinearAndClamped() {
        assertEquals(10L, EffectPolicy.watchmanInterval(0.0, 45.0, 10, 80));
        assertEquals(45L, EffectPolicy.watchmanInterval(22.5, 45.0, 10, 80));
        assertEquals(80L, EffectPolicy.watchmanInterval(45.0, 45.0, 10, 80));
        assertEquals(80L, EffectPolicy.watchmanInterval(100.0, 45.0, 10, 80));
    }

    @Test
    void potionRefreshDoesNotOverwriteStrongerOrHealthySameLevelEffects() {
        assertFalse(EffectPolicy.shouldApplyPotion(2, 200, 1, 40));
        assertFalse(EffectPolicy.shouldApplyPotion(1, 80, 1, 40));
        assertTrue(EffectPolicy.shouldApplyPotion(1, 20, 1, 40));
        assertTrue(EffectPolicy.shouldApplyPotion(0, 200, 1, 40));
        assertTrue(EffectPolicy.shouldApplyPotion(1, 80, 1, 99));
        assertFalse(EffectPolicy.shouldApplyPotion(1, 100, 1, 99));
    }
}
