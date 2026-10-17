package co.surumene.www.behavior;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfRelationshipRuntimeTest {
    @Test
    void passiveRollsOnlyOnDeadlinesAndModeChangesRestartTheInterval() {
        World world = BehaviorPaperTestSupport.world(UUID.randomUUID());
        UUID commander = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var wolf = BehaviorPaperTestSupport.wolf(
                UUID.randomUUID(), world, new BehaviorPaperTestSupport.MemoryPdc(),
                new Location(world, 0, 64, 0), false).wolf();
        var loaded = BehaviorPaperTestSupport.loaded();
        var source = BehaviorTestIndividuals.individual(0, 7).withCommandState(
                Mode.FOLLOW, Optional.of(commander), ActionDistance.NORMAL, Optional.empty());
        loaded.saveAndRegister(wolf, source);

        var relationships = new WonderfulWolfRelationshipService(loaded);
        AtomicInteger drawCalls = new AtomicInteger();
        var runtime = new WonderfulWolfRelationshipRuntime(
                loaded, relationships,
                () -> new WwwConfig.Relationship(20, 1.0),
                () -> {
                    drawCalls.incrementAndGet();
                    return 0.0;
                },
                new PendingFeedTracker(), Logger.getAnonymousLogger());

        runtime.tick(5L);
        runtime.tick(10L);
        runtime.tick(15L);
        runtime.tick(20L);
        assertEquals(0, drawCalls.get());
        assertEquals(0, relationships.current(wolf, commander).orElseThrow());

        runtime.tick(25L);
        assertEquals(1, drawCalls.get());
        assertEquals(7, relationships.current(wolf, commander).orElseThrow());
        runtime.tick(30L);
        assertEquals(1, drawCalls.get());

        loaded.saveAndRegister(wolf, source.withCommandState(
                Mode.WANDER, Optional.empty(), ActionDistance.NORMAL, Optional.empty()));
        runtime.tick(35L);
        loaded.saveAndRegister(wolf, source.withCommandState(
                Mode.FOLLOW, Optional.of(commander), ActionDistance.NORMAL, Optional.empty()));
        runtime.tick(40L);
        runtime.tick(55L);
        assertEquals(1, drawCalls.get());
        runtime.tick(60L);
        assertEquals(2, drawCalls.get());
    }

    @Test
    void configReloadRecomputesDueTimeWithoutResettingElapsedTicks() {
        World world = BehaviorPaperTestSupport.world(UUID.randomUUID());
        UUID commander = UUID.randomUUID();
        var wolf = BehaviorPaperTestSupport.wolf(
                UUID.randomUUID(), world, new BehaviorPaperTestSupport.MemoryPdc(),
                new Location(world, 0, 64, 0), false).wolf();
        var loaded = BehaviorPaperTestSupport.loaded();
        loaded.saveAndRegister(wolf, BehaviorTestIndividuals.individual(0, 7)
                .withCommandState(Mode.GUARD, Optional.of(commander),
                        ActionDistance.NORMAL, Optional.empty()));
        AtomicReference<WwwConfig.Relationship> config =
                new AtomicReference<>(new WwwConfig.Relationship(20, 1.0));
        AtomicInteger draws = new AtomicInteger();
        var runtime = new WonderfulWolfRelationshipRuntime(
                loaded, new WonderfulWolfRelationshipService(loaded), config::get,
                () -> {
                    draws.incrementAndGet();
                    return 0.0;
                },
                new PendingFeedTracker(), Logger.getAnonymousLogger());

        runtime.tick(5L);
        runtime.tick(25L);
        assertEquals(1, draws.get());

        config.set(new WwwConfig.Relationship(40, 1.0));
        runtime.onConfigReload();
        runtime.tick(30L);
        runtime.tick(45L);
        assertEquals(1, draws.get());
        runtime.tick(65L);
        assertEquals(2, draws.get());
    }
}
