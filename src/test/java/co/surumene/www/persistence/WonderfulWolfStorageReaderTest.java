package co.surumene.www.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.surumene.www.WhatAWonderfulWolfPlugin;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wcl.api.inventory.StorageReadResult;
import co.surumene.wcl.api.inventory.StorageSnapshot;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261108-007: loaded WWW storage provider contract characterization.
 * This test does not claim an actual Paper server or complete offline inventory scan.
 */
class WonderfulWolfStorageReaderTest {
    @Test void loadedWolfExportsWorldPositionAndBothRelationshipsWithoutWritingEntity() {
        var plugin = mock(WhatAWonderfulWolfPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        var loaded = mock(WonderfulWolfLoadedIndividuals.class);
        var wolf = mock(Wolf.class);
        var state = mock(WonderfulWolfIndividual.class);
        var world = mock(World.class);
        var location = mock(Location.class);
        UUID wolfId = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID commander = UUID.randomUUID();

        when(loaded.entity(wolfId)).thenReturn(Optional.of(wolf));
        when(loaded.find(wolfId)).thenReturn(Optional.of(state));
        when(wolf.getWorld()).thenReturn(world);
        when(world.getUID()).thenReturn(worldId);
        when(wolf.getLocation()).thenReturn(location);
        when(location.getX()).thenReturn(1.25);
        when(location.getY()).thenReturn(70.0);
        when(location.getZ()).thenReturn(-99.5);
        when(state.ownerId()).thenReturn(Optional.of(owner));
        when(state.commanderId()).thenReturn(Optional.of(commander));
        when(state.weapon()).thenReturn(Optional.empty());
        when(state.inventory()).thenReturn(Map.of());

        var result = new WonderfulWolfStorageReader(plugin, loaded).readLoaded(wolfId);
        StorageSnapshot snapshot = assertInstanceOf(StorageReadResult.Success.class, result).snapshot();
        assertEquals(wolfId, snapshot.entityId());
        assertEquals("www", snapshot.providerId());
        assertEquals(worldId, snapshot.worldId());
        assertEquals(Optional.of(new StorageSnapshot.Position(1.25, 70.0, -99.5)),
                snapshot.position());
        assertEquals(Map.of("owner", owner, "commander", commander), snapshot.relationships());
        assertTrue(snapshot.items().isEmpty());
        verify(wolf, never()).getPersistentDataContainer();
    }

    @Test void missingLoadedIndividualCannotBecomeAZeroValueSuccess() {
        var plugin = mock(WhatAWonderfulWolfPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        var loaded = mock(WonderfulWolfLoadedIndividuals.class);
        var wolf = mock(Wolf.class);
        UUID wolfId = UUID.randomUUID();
        when(loaded.entity(wolfId)).thenReturn(Optional.empty());

        var reader = new WonderfulWolfStorageReader(plugin, loaded);
        assertInstanceOf(StorageReadResult.NotFound.class, reader.readLoaded(wolfId));

        when(loaded.entity(wolfId)).thenReturn(Optional.of(wolf));
        when(loaded.find(wolfId)).thenReturn(Optional.empty());
        var missing = assertInstanceOf(StorageReadResult.Failure.class, reader.readLoaded(wolfId));
        assertEquals("MISSING_DATA", missing.code());

        when(plugin.isEnabled()).thenReturn(false);
        var stopped = assertInstanceOf(StorageReadResult.Failure.class, reader.readLoaded(wolfId));
        assertEquals("DISABLED", stopped.code());
    }
}
