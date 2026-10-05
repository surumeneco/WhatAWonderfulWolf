package co.surumene.www.spawn;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

final class NaturalWolfSpawnListenerTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();

    @Test
    void naturalCreatureSpawnCanConvertButBreedingAndCommandNeverDo() {
        List<UUID> converted = new ArrayList<>();
        NaturalWolfConverter converter = (wolf, seed) -> {
            converted.add(wolf.getUniqueId());
            return new WonderfulWolfEntityCreationResult.Success(
                    wolf,
                    TestIndividuals.individual());
        };
        AtomicLong seeds = new AtomicLong(10L);
        WonderfulWolfNaturalSpawnListener listener =
                new WonderfulWolfNaturalSpawnListener(
                        converter,
                        () -> config.runtime().spawn(),
                        () -> 0.0,
                        seeds::incrementAndGet,
                        Logger.getLogger("test"));

        Wolf natural = wolf(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        listener.onCreatureSpawn(new CreatureSpawnEvent(
                natural,
                CreatureSpawnEvent.SpawnReason.NATURAL));
        listener.onCreatureSpawn(new CreatureSpawnEvent(
                wolf(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                CreatureSpawnEvent.SpawnReason.BREEDING));
        listener.onCreatureSpawn(new CreatureSpawnEvent(
                wolf(UUID.fromString("33333333-3333-3333-3333-333333333333")),
                CreatureSpawnEvent.SpawnReason.COMMAND));

        assertEquals(List.of(natural.getUniqueId()), converted);
    }

    @Test
    void newlyGeneratedChunkWolvesUseNaturalConversionPathButReloadedChunksDoNot() {
        List<UUID> converted = new ArrayList<>();
        NaturalWolfConverter converter = (wolf, seed) -> {
            converted.add(wolf.getUniqueId());
            return new WonderfulWolfEntityCreationResult.Success(
                    wolf,
                    TestIndividuals.individual());
        };
        WonderfulWolfNaturalSpawnListener listener =
                new WonderfulWolfNaturalSpawnListener(
                        converter,
                        () -> config.runtime().spawn(),
                        () -> 0.0,
                        () -> 99L,
                        Logger.getLogger("test"));

        Wolf first = wolf(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        Wolf second = wolf(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        Entity nonWolf = entity();
        Chunk newChunk = chunk(new Entity[]{first, nonWolf, second});

        listener.onChunkLoad(new ChunkLoadEvent(newChunk, true));
        listener.onChunkLoad(new ChunkLoadEvent(
                chunk(new Entity[]{wolf(UUID.fromString(
                        "66666666-6666-6666-6666-666666666666"))}),
                false));

        assertEquals(List.of(first.getUniqueId(), second.getUniqueId()), converted);
    }

    private static Wolf wolf(UUID id) {
        return (Wolf) Proxy.newProxyInstance(
                Wolf.class.getClassLoader(),
                new Class<?>[]{Wolf.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Wolf[" + id + "]";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Entity entity() {
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[]{Entity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Entity";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Chunk chunk(Entity[] entities) {
        return (Chunk) Proxy.newProxyInstance(
                Chunk.class.getClassLoader(),
                new Class<?>[]{Chunk.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEntities" -> entities.clone();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Chunk";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\0';
        throw new AssertionError(type);
    }
}
