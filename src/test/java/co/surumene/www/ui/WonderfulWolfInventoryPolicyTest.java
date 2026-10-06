package co.surumene.www.ui;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfInventoryPolicyTest {
    @Test
    void managementAndCargoSlotsAreSeparated() {
        assertTrue(WonderfulWolfInventoryPolicy.isManagementSlot(0));
        assertTrue(WonderfulWolfInventoryPolicy.isManagementSlot(8));
        assertFalse(WonderfulWolfInventoryPolicy.isManagementSlot(9));
        assertTrue(WonderfulWolfInventoryPolicy.isUsableCargoSlot(9, 1));
        assertFalse(WonderfulWolfInventoryPolicy.isUsableCargoSlot(10, 1));
        assertFalse(WonderfulWolfInventoryPolicy.isUsableCargoSlot(8, 45));
        assertTrue(WonderfulWolfInventoryPolicy.isUsableCargoSlot(53, 45));
    }

    @Test
    void leftAndRightClicksCycleModesInOppositeDirections() {
        assertEquals(Mode.FOLLOW, WonderfulWolfInventoryPolicy.cycleMode(Mode.WANDER, true));
        assertEquals(Mode.WAIT, WonderfulWolfInventoryPolicy.cycleMode(Mode.WANDER, false));
        assertEquals(Mode.WANDER, WonderfulWolfInventoryPolicy.cycleMode(Mode.WAIT, true));
    }

    @Test
    void leftAndRightClicksCycleActionDistanceInOppositeDirections() {
        assertEquals(ActionDistance.NORMAL,
                WonderfulWolfInventoryPolicy.cycleActionDistance(ActionDistance.NARROW, true));
        assertEquals(ActionDistance.VERY_WIDE,
                WonderfulWolfInventoryPolicy.cycleActionDistance(ActionDistance.NARROW, false));
        assertEquals(ActionDistance.NARROW,
                WonderfulWolfInventoryPolicy.cycleActionDistance(ActionDistance.VERY_WIDE, true));
    }
}
