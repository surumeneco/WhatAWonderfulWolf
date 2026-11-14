package co.surumene.www.wolftrap;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfCreationResult;
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
        assertFalse(runtime.protectDamage(bait, false));
        assertTrue(runtime.activateByAttack(bait));
        verify(factory, times(2)).spawnPreparedFounder(any(Location.class), isNull());
    }
}
