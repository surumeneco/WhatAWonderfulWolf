package co.surumene.www.behavior;

import co.surumene.www.persistence.RestoreResult;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfFirstCreationTest {
    @Test
    void initializesOnlyFirstSavedEntityAndNeverOnRestoreOrUpdate() {
        World world = BehaviorPaperTestSupport.world(UUID.randomUUID());
        UUID entityId = UUID.randomUUID();
        var pdc = new BehaviorPaperTestSupport.MemoryPdc();
        var fixture = BehaviorPaperTestSupport.wolf(
                entityId, world, pdc, new Location(world, 0, 64, 0), false);
        WonderfulWolfLoadedIndividuals loaded = BehaviorPaperTestSupport.loaded();
        AtomicInteger calls = new AtomicInteger();
        loaded.onFirstCreation(wolf -> calls.incrementAndGet());

        var individual = BehaviorTestIndividuals.individual(0, 10);
        loaded.saveAndRegister(fixture.wolf(), individual);
        assertEquals(1, calls.get(), "first materialization must initialize");

        loaded.saveAndRegister(fixture.wolf(), individual);
        assertEquals(1, calls.get(), "PDC update must not heal a wounded wolf");

        loaded.unregister(fixture.wolf());
        var replacement = BehaviorPaperTestSupport.wolf(
                entityId, world, pdc, new Location(world, 2, 64, 0), false);
        assertInstanceOf(
                RestoreResult.Success.class, loaded.register(replacement.wolf()));
        assertEquals(1, calls.get(), "PDC restoration must not heal a wounded wolf");
    }
}
