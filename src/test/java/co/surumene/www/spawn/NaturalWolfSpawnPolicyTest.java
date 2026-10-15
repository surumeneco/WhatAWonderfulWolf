package co.surumene.www.spawn;

import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class NaturalWolfSpawnPolicyTest {
    @Test
    void onlyNaturalCreatureSpawnsUseTheNaturalConversionProbability() {
        assertTrue(NaturalWolfSpawnPolicy.shouldConvert(
                CreatureSpawnEvent.SpawnReason.NATURAL, 0.05, 0.049999));
        assertFalse(NaturalWolfSpawnPolicy.shouldConvert(
                CreatureSpawnEvent.SpawnReason.NATURAL, 0.05, 0.05));
        assertFalse(NaturalWolfSpawnPolicy.shouldConvert(
                CreatureSpawnEvent.SpawnReason.BREEDING, 1.0, 0.0));
        assertFalse(NaturalWolfSpawnPolicy.shouldConvert(
                CreatureSpawnEvent.SpawnReason.COMMAND, 1.0, 0.0));
        assertFalse(NaturalWolfSpawnPolicy.shouldConvert(
                CreatureSpawnEvent.SpawnReason.SPAWNER, 1.0, 0.0));
    }

    @Test
    void initialWorldGenerationUsesTheSameProbabilityOnlyForNewChunks() {
        assertTrue(NaturalWolfSpawnPolicy.shouldConvertGeneratedWolf(
                true, 0.05, 0.049999));
        assertFalse(NaturalWolfSpawnPolicy.shouldConvertGeneratedWolf(
                true, 0.05, 0.05));
        assertFalse(NaturalWolfSpawnPolicy.shouldConvertGeneratedWolf(
                false, 1.0, 0.0));
    }

    @Test
    void probabilityAndDrawBoundariesAreValidated() {
        assertThrows(IllegalArgumentException.class, () ->
                NaturalWolfSpawnPolicy.shouldConvert(
                        CreatureSpawnEvent.SpawnReason.NATURAL, -0.01, 0.5));
        assertThrows(IllegalArgumentException.class, () ->
                NaturalWolfSpawnPolicy.shouldConvertGeneratedWolf(
                        true, 1.01, 0.5));
        assertThrows(IllegalArgumentException.class, () ->
                NaturalWolfSpawnPolicy.shouldConvertGeneratedWolf(
                        true, 0.5, 1.0));
    }
}
