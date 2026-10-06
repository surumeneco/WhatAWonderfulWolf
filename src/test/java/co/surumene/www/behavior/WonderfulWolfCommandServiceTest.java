package co.surumene.www.behavior;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfCommandServiceTest {
    @Test
    void waitModeCapturesWolfLocationAndPersistsAcrossEntityReplacement() {
        UUID worldId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID wolfId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID commander = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        World world = BehaviorPaperTestSupport.world(worldId);
        BehaviorPaperTestSupport.MemoryPdc pdc =
                new BehaviorPaperTestSupport.MemoryPdc();
        var fixture = BehaviorPaperTestSupport.wolf(
                wolfId,
                world,
                pdc,
                new Location(world, 12.5, 64.0, -8.25),
                true);

        WonderfulWolfLoadedIndividuals loaded =
                BehaviorPaperTestSupport.loaded();
        loaded.saveAndRegister(
                fixture.wolf(),
                BehaviorTestIndividuals.individual(0, 10));

        ManualTargetRegistry manual = new ManualTargetRegistry();
        AtomicInteger changed = new AtomicInteger();
        WonderfulWolfCommandService service =
                new WonderfulWolfCommandService(
                        loaded,
                        manual,
                        ignored -> changed.incrementAndGet());

        assertTrue(service.changeMode(
                fixture.wolf(),
                Mode.WAIT,
                commander));
        assertFalse(fixture.sitting().get());
        assertTrue(fixture.stopCalls().get() > 0);
        assertEquals(1, changed.get());

        var current = loaded.find(wolfId).orElseThrow();
        assertEquals(Mode.WAIT, current.mode());
        assertEquals(commander, current.commanderId().orElseThrow());
        assertEquals(worldId, current.waitLocation().orElseThrow().worldId());
        assertEquals(12.5, current.waitLocation().orElseThrow().x(), 1.0e-12);
        assertEquals(-8.25, current.waitLocation().orElseThrow().z(), 1.0e-12);

        loaded.clear();
        var replacement = BehaviorPaperTestSupport.wolf(
                wolfId,
                world,
                pdc,
                new Location(world, 0, 0, 0),
                false);
        WonderfulWolfLoadedIndividuals restarted =
                BehaviorPaperTestSupport.loaded();
        RestoreResult.Success restored = assertInstanceOf(
                RestoreResult.Success.class,
                restarted.register(replacement.wolf()));

        assertEquals(Mode.WAIT, restored.individual().mode());
        assertEquals(
                commander,
                restored.individual().commanderId().orElseThrow());
        assertEquals(
                current.waitLocation(),
                restored.individual().waitLocation());
    }

    @Test
    void actionDistanceChangePersistsWithoutChangingMode() {
        UUID worldId = UUID.randomUUID();
        UUID wolfId = UUID.randomUUID();
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
                BehaviorTestIndividuals.individual(0, 10));

        WonderfulWolfCommandService service =
                new WonderfulWolfCommandService(
                        loaded,
                        new ManualTargetRegistry(),
                        ignored -> {});

        assertTrue(service.changeActionDistance(
                fixture.wolf(),
                ActionDistance.VERY_WIDE));

        var current = loaded.find(wolfId).orElseThrow();
        assertEquals(Mode.WANDER, current.mode());
        assertEquals(ActionDistance.VERY_WIDE, current.actionDistance());
    }
}
