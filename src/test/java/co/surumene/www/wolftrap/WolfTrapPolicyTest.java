package co.surumene.www.wolftrap;

import org.bukkit.event.weather.LightningStrikeEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WolfTrapPolicyTest {
    @Test
    void onlyWeatherLightningCanStartNaturalTrap() {
        assertTrue(WolfTrapPolicy.isNaturalLightning(LightningStrikeEvent.Cause.WEATHER));
        assertFalse(WolfTrapPolicy.isNaturalLightning(LightningStrikeEvent.Cause.TRIDENT));
        assertFalse(WolfTrapPolicy.isNaturalLightning(LightningStrikeEvent.Cause.COMMAND));
        assertFalse(WolfTrapPolicy.isNaturalLightning(LightningStrikeEvent.Cause.CUSTOM));
    }

    @Test
    void additionalTrapRequiresNaturalAdvanceToExactly18000() {
        assertTrue(WolfTrapPolicy.reachedAdditionalCheckTime(17999L, 18000L));
        assertFalse(WolfTrapPolicy.reachedAdditionalCheckTime(17990L, 18000L));
        assertFalse(WolfTrapPolicy.reachedAdditionalCheckTime(17999L, 18001L));
        assertFalse(WolfTrapPolicy.reachedAdditionalCheckTime(41999L, 42001L));
        assertTrue(WolfTrapPolicy.reachedAdditionalCheckTime(41999L, 42000L));
    }

    @Test
    void probabilityUsesHalfOpenUnitInterval() {
        assertTrue(WolfTrapPolicy.roll(0.0, 0.01));
        assertTrue(WolfTrapPolicy.roll(0.009999, 0.01));
        assertFalse(WolfTrapPolicy.roll(0.01, 0.01));
        assertFalse(WolfTrapPolicy.roll(0.9, 0.0));
        assertTrue(WolfTrapPolicy.roll(0.999999, 1.0));
    }

    @Test
    void graceIsActiveUntilDeadlineButNotAtDeadline() {
        assertTrue(WolfTrapPolicy.inGrace(100L, 160L));
        assertTrue(WolfTrapPolicy.inGrace(159L, 160L));
        assertFalse(WolfTrapPolicy.inGrace(160L, 160L));
        assertFalse(WolfTrapPolicy.inGrace(161L, 160L));
    }

    @Test
    void randomOffsetNeverExceedsConfiguredRadius() {
        WolfTrapPolicy.Offset center = WolfTrapPolicy.offset(0.0, 0.25, 32.0);
        assertEquals(0.0, center.x(), 1.0e-12);
        assertEquals(0.0, center.z(), 1.0e-12);

        WolfTrapPolicy.Offset edge = WolfTrapPolicy.offset(1.0, 0.0, 32.0);
        assertEquals(32.0, Math.hypot(edge.x(), edge.z()), 1.0e-9);
    }
}
