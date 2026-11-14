package co.surumene.www.wolftrap;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfCreationResult;
import co.surumene.www.spawn.WonderfulWolfEntityCreationResult;
import co.surumene.www.individual.WonderfulWolfIndividual;
import org.bukkit.entity.Wolf;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Evoker;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WolfTrapRuntimePreparationTest {
    @Test
    void preparationProtectsBaitAndUnlocksOnlyAfterBothFoundersAreReady() {
        WolfTrapStateStore states = mock(WolfTrapStateStore.class);
        PaperWonderfulWolfFactory factory = mock(PaperWonderfulWolfFactory.class);
        Server server = mock(Server.class);
        WwwConfig.Runtime config = mock(WwwConfig.Runtime.class);
        Evoker bait = mock(Evoker.class);
        World world = mock(World.class);
        UUID id = UUID.randomUUID();
        when(bait.getUniqueId()).thenReturn(id);
        when(bait.isValid()).thenReturn(true);
        when(states.isArmed(bait)).thenReturn(true);
        when(server.getEntity(id)).thenReturn(bait);
        when(bait.getWorld()).thenReturn(world);
        when(bait.getLocation()).thenReturn(new Location(world, 2, 65, 3));

        AtomicReference<Consumer<PaperWonderfulWolfFactory.PreparedPair>> callback =
                new AtomicReference<>();
        when(factory.prepareTrapPairAsync(anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    callback.set(invocation.getArgument(2));
                    return true;
                });

        WolfTrapRuntime runtime = new WolfTrapRuntime(states, factory,
                mock(WonderfulWolfLoadedIndividuals.class),
                mock(WonderfulWolfBehaviorRuntime.class), () -> config, server,
                () -> 0.25, () -> 42, System::currentTimeMillis,
                Logger.getLogger("wolf-trap-test"));

        runtime.register(bait);
        assertNotNull(callback.get());
        assertTrue(runtime.protectDamage(bait, false));
        verifyNoInteractions(world);
        assertFalse(runtime.activateByAttack(bait));
        verify(factory, never()).spawnPreparedFounder(any(), any());

        callback.get().accept(new PaperWonderfulWolfFactory.PreparedPair(
                mock(WonderfulWolfCreationResult.Success.class),
                mock(WonderfulWolfCreationResult.Success.class)));
        Wolf firstWolf = mock(Wolf.class);
        Wolf secondWolf = mock(Wolf.class);
        Evoker secondRider = mock(Evoker.class);
        WonderfulWolfIndividual firstIndividual = mock(WonderfulWolfIndividual.class);
        WonderfulWolfIndividual secondIndividual = mock(WonderfulWolfIndividual.class);
        when(factory.spawnPreparedFounder(any(Location.class), isNull()))
                .thenReturn(new WonderfulWolfEntityCreationResult.Success(firstWolf, firstIndividual))
                .thenReturn(new WonderfulWolfEntityCreationResult.Success(secondWolf, secondIndividual));
        when(world.spawn(any(Location.class), eq(Evoker.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM)))
                .thenReturn(secondRider);
        when(firstWolf.addPassenger(bait)).thenReturn(true);
        when(secondWolf.addPassenger(secondRider)).thenReturn(true);
        assertFalse(runtime.protectDamage(bait, false));
        assertTrue(runtime.activateByAttack(bait));
        verify(factory, times(2)).spawnPreparedFounder(any(Location.class), isNull());
        verify(states).markRider(eq(bait), any(UUID.class));
        verify(states).markMountedWolf(eq(firstWolf), eq(id));
    }

    @Test
    void failedSecondSpawnDiscardsFirstWolfBeforeRetry() {
        WolfTrapStateStore states = mock(WolfTrapStateStore.class);
        PaperWonderfulWolfFactory factory = mock(PaperWonderfulWolfFactory.class);
        WonderfulWolfLoadedIndividuals loaded = mock(WonderfulWolfLoadedIndividuals.class);
        Server server = mock(Server.class);
        WwwConfig.Runtime config = mock(WwwConfig.Runtime.class);
        Evoker bait = mock(Evoker.class);
        World world = mock(World.class);
        Wolf firstWolf = mock(Wolf.class);
        WonderfulWolfIndividual firstIndividual = mock(WonderfulWolfIndividual.class);
        WonderfulWolfIndividual secondIndividual = mock(WonderfulWolfIndividual.class);
        UUID id = UUID.randomUUID();
        when(bait.getUniqueId()).thenReturn(id);
        when(bait.isValid()).thenReturn(true);
        when(states.isArmed(bait)).thenReturn(true);
        when(server.getEntity(id)).thenReturn(bait);
        when(bait.getWorld()).thenReturn(world);
        when(bait.getLocation()).thenReturn(new Location(world, 2, 65, 3));

        AtomicReference<Consumer<PaperWonderfulWolfFactory.PreparedPair>> callback =
                new AtomicReference<>();
        when(factory.prepareTrapPairAsync(anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    callback.set(invocation.getArgument(2));
                    return true;
                });
        WolfTrapRuntime runtime = new WolfTrapRuntime(states, factory, loaded,
                mock(WonderfulWolfBehaviorRuntime.class), () -> config, server,
                () -> 0.25, () -> 42, System::currentTimeMillis,
                Logger.getLogger("wolf-trap-rollback-test"));

        runtime.register(bait);
        WonderfulWolfCreationResult.Success firstFounder =
                mock(WonderfulWolfCreationResult.Success.class);
        WonderfulWolfCreationResult.Success secondFounder =
                mock(WonderfulWolfCreationResult.Success.class);
        when(firstFounder.individual()).thenReturn(firstIndividual);
        when(secondFounder.individual()).thenReturn(secondIndividual);
        callback.get().accept(new PaperWonderfulWolfFactory.PreparedPair(
                firstFounder, secondFounder));
        when(factory.spawnPreparedFounder(any(Location.class), any(WonderfulWolfIndividual.class)))
                .thenReturn(new WonderfulWolfEntityCreationResult.Success(firstWolf, firstIndividual))
                .thenThrow(new IllegalStateException("second spawn failed"));

        assertTrue(runtime.activateByAttack(bait));
        // Failed conversion must not strand a registered trap Wolf in the world.
        verify(loaded).unregister(firstWolf);
        verify(firstWolf).remove();
        verify(states).markArmed(eq(bait), anyLong());
    }
}
