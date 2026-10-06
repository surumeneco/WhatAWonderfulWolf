package co.surumene.www.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfWeaponTimingTest {
    @Test
    void convertsAttacksPerSecondToWholeTickCooldown() {
        assertEquals(10L, WonderfulWolfWeaponTiming.cooldownTicks(2.0));
        assertEquals(14L, WonderfulWolfWeaponTiming.cooldownTicks(1.5));
        assertEquals(100L, WonderfulWolfWeaponTiming.cooldownTicks(0.2));
    }

    @Test
    void rejectsNonFiniteOrNegativeAttackSpeed() {
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfWeaponTiming.cooldownTicks(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfWeaponTiming.cooldownTicks(-0.1));
    }

    @Test
    void zeroAttackSpeedCannotProduceAnAttack() {
        assertEquals(Long.MAX_VALUE, WonderfulWolfWeaponTiming.cooldownTicks(0.0));
    }
}
