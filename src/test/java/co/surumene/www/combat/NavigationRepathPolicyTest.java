package co.surumene.www.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NavigationRepathPolicyTest {
    @Test void keepsCurrentPathForStationaryTarget() {
        assertFalse(NavigationRepathPolicy.replan(true, false, 0.0));
        assertFalse(NavigationRepathPolicy.replan(true, false, 0.24));
    }

    @Test void replansWhenLostChangedOrMoved() {
        assertTrue(NavigationRepathPolicy.replan(false, false, 0.0));
        assertTrue(NavigationRepathPolicy.replan(true, true, 0.0));
        assertTrue(NavigationRepathPolicy.replan(true, false, 0.25));
    }
}
