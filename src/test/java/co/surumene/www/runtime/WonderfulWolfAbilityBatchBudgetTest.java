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
    void skipsEvaluationOnlyWhenAgeEffectsAndInjuriesAreUnchanged() {
        var values = new java.util.EnumMap<
                co.surumene.www.domain.Ability, co.surumene.www.ability.EffectiveAbility>(
                co.surumene.www.domain.Ability.class);
        for (var ability : co.surumene.www.domain.Ability.values()) {
            values.put(ability, new co.surumene.www.ability.EffectiveAbility(
                    0.5, 1.0, 0.5, 1.0,
                    co.surumene.www.ability.AbilityRank.COMMON, false));
        }
        var curve = new co.surumene.www.ability.AgeCurve(
                100, 0.5, 150, 200, 2.0);
        var previous = new co.surumene.www.ability.EffectiveAbilities(
                values, 105, curve);
        assertTrue(WonderfulWolfAbilityRuntime.sameAgeModifiers(
                previous, 110, java.util.List.of()));
        assertFalse(WonderfulWolfAbilityRuntime.sameAgeModifiers(
                previous, 160, java.util.List.of()));
        var injury = new co.surumene.www.domain.InjuryPhenotype(
                co.surumene.www.domain.Ability.HEALTH, 108, 1.0);
        assertFalse(WonderfulWolfAbilityRuntime.sameAgeModifiers(
                previous, 110, java.util.List.of(injury)));
    }

    @Test
    void negativeCountFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfAbilityRuntime.perTickBudget(-1));
    }
}
