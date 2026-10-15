package co.surumene.www.behavior;

import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfRelationshipServiceTest {
    @Test
    void rewardAndPenaltyPersistThroughPhase4Pdc() {
        UUID worldId = UUID.randomUUID();
        UUID wolfId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        World world = BehaviorPaperTestSupport.world(worldId);
        BehaviorPaperTestSupport.MemoryPdc pdc =
                new BehaviorPaperTestSupport.MemoryPdc();
        var fixture = BehaviorPaperTestSupport.wolf(
                wolfId,
                world,
                pdc,
                new Location(world, 0, 64, 0),
                false);

        WonderfulWolfLoadedIndividuals loaded =
                BehaviorPaperTestSupport.loaded();
        loaded.saveAndRegister(
                fixture.wolf(),
                BehaviorTestIndividuals.individual(-20, 7));
        WonderfulWolfRelationshipService service =
                new WonderfulWolfRelationshipService(loaded);

        assertEquals(-20L, service.current(
                fixture.wolf(),
                playerId).orElseThrow());
        assertTrue(service.reward(fixture.wolf(), playerId));
        assertEquals(-13L, service.current(
                fixture.wolf(),
                playerId).orElseThrow());
        assertTrue(service.penalize(fixture.wolf(), playerId));
        assertEquals(-20L, service.current(
                fixture.wolf(),
                playerId).orElseThrow());

        loaded.clear();
        var replacement = BehaviorPaperTestSupport.wolf(
                wolfId,
                world,
                pdc,
                new Location(world, 0, 64, 0),
                false);
        WonderfulWolfLoadedIndividuals restarted =
                BehaviorPaperTestSupport.loaded();
        RestoreResult.Success restored = assertInstanceOf(
                RestoreResult.Success.class,
                restarted.register(replacement.wolf()));
        assertEquals(
                -20L,
                restored.individual().affection().get(playerId));
    }
}
