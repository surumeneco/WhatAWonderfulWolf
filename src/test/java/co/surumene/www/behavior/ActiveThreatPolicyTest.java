package co.surumene.www.behavior;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ActiveThreatPolicyTest {
    @Test
    void conditionallyNeutralEnemyTypesRequireActualHostility() {
        for (EntityType type : new EntityType[]{
                EntityType.ENDERMAN,
                EntityType.PIGLIN,
                EntityType.ZOMBIFIED_PIGLIN,
                EntityType.SPIDER}) {
            assertFalse(ActiveThreatPolicy.isActiveThreat(type, true, false));
            assertTrue(ActiveThreatPolicy.isActiveThreat(type, true, true));
        }
    }

    @Test
    void alwaysHostileEnemyTypesCanBeProactivelySelected() {
        assertTrue(ActiveThreatPolicy.isActiveThreat(
                EntityType.HOGLIN, true, false));
        assertTrue(ActiveThreatPolicy.isActiveThreat(
                EntityType.ZOMBIE, true, false));
    }

    @Test
    void nonEnemyMobsOnlyQualifyWhenActuallyTargetingTheCommandChain() {
        assertFalse(ActiveThreatPolicy.isActiveThreat(
                EntityType.IRON_GOLEM, false, false));
        assertTrue(ActiveThreatPolicy.isActiveThreat(
                EntityType.IRON_GOLEM, false, true));
    }
}
