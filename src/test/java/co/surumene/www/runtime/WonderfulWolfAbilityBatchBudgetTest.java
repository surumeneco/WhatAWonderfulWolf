package co.surumene.www.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfAbilityBatchBudgetTest {
    @Test
    void spreadsAnyLoadedPopulationAcrossTwentyTicks() {
        for (int count : new int[] {0, 1, 19, 20, 21, 100, 101, 1_000}) {
            int perTick = WonderfulWolfAbilityRuntime.perTickBudget(count);
            assertTrue(perTick >= 1);
            assertTrue(20 * perTick >= count);
            if (count > 20) assertTrue(perTick < count);
        }
    }

    @Test
    void negativeCountFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfAbilityRuntime.perTickBudget(-1));
    }
}
