package co.surumene.www.persistence;

import co.surumene.www.domain.*;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class PaperWonderfulWolfEntityStoreTest {
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());

    @Test
    void writesToRealPaperPdcTypesAndRestoresFromTheWolfEntity() {
        Plugin plugin = plugin();
        MemoryPdc memory = new MemoryPdc();
        Wolf wolf = wolf(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                memory.proxy());

        WonderfulWolfEntityStore store = new WonderfulWolfEntityStore(plugin, engine);
        WonderfulWolfIndividual source = individual();

        assertFalse(store.isWonderful(wolf));
        store.save(wolf, source);
        assertTrue(store.isWonderful(wolf));

        RestoreResult.Success restored = assertInstanceOf(
                RestoreResult.Success.class,
                store.restore(wolf));
        assertEquals(source, restored.individual());

        assertTrue(memory.values.keySet().stream()
                .map(NamespacedKey::toString)
                .anyMatch("whatawonderfulwolf:type"::equals));
        assertTrue(memory.values.keySet().stream()
                .map(NamespacedKey::toString)
                .anyMatch("whatawonderfulwolf:genome"::equals));
    }

    @Test
    void nonWolfEntitiesAreNeverClassifiedAsWonderfulByTheEntityStore() {
        Plugin plugin = plugin();
        WonderfulWolfEntityStore store = new WonderfulWolfEntityStore(plugin, engine);

        assertInstanceOf(
                RestoreResult.NotWonderful.class,
                store.restore(null));
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "WhatAWonderfulWolf";
                    case "toString" -> "WhatAWonderfulWolf";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Wolf wolf(UUID id, PersistentDataContainer pdc) {
        return (Wolf) Proxy.newProxyInstance(
                Wolf.class.getClassLoader(),
                new Class<?>[]{Wolf.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPersistentDataContainer" -> pdc;
                    case "getUniqueId" -> id;
                    case "toString" -> "Wolf[" + id + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
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

    private static final class MemoryPdc {
        private final Map<NamespacedKey, Object> values = new HashMap<>();

        PersistentDataContainer proxy() {
            return (PersistentDataContainer) Proxy.newProxyInstance(
                    PersistentDataContainer.class.getClassLoader(),
                    new Class<?>[]{PersistentDataContainer.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "get" -> values.get((NamespacedKey) args[0]);
                        case "set" -> {
                            Object value = args[2];
                            if (value instanceof byte[] bytes) value = bytes.clone();
                            values.put((NamespacedKey) args[0], value);
                            yield null;
                        }
                        case "remove" -> {
                            values.remove((NamespacedKey) args[0]);
                            yield null;
                        }
                        case "has" -> values.containsKey((NamespacedKey) args[0]);
                        case "getKeys" -> Set.copyOf(values.keySet());
                        case "isEmpty" -> values.isEmpty();
                        case "getSize" -> values.size();
                        case "toString" -> values.toString();
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private static WonderfulWolfIndividual individual() {
        UUID owner = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID commander = UUID.fromString("20000000-0000-0000-0000-000000000002");
        return new WonderfulWolfIndividual(
                genome(),
                phenotype(),
                Optional.of(owner),
                100L,
                Mode.FOLLOW,
                Optional.of(commander),
                ActionDistance.NORMAL,
                Optional.empty(),
                Map.of(owner, 50L),
                Optional.empty(),
                Map.of(),
                0,
                PedigreeSnapshot.founder());
    }

    private static DiploidGenome genome() {
        BitSequence bits = BitSequence.fromBits("10101010");
        return new DiploidGenome(1, List.of(new ChromosomePair(bits, bits)));
    }

    private static PhenotypeSnapshot phenotype() {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 0.5);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) personality.put(factor, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) development.put(factor, 0.5);
        return new PhenotypeSnapshot(
                new DecoderIdentity(
                        1,
                        new byte[32],
                        new ProfileDescriptor("wonderful-wolf", 1, new byte[32])),
                abilities,
                new RelationshipPerformance(0, 10),
                personality,
                Personality.SERIOUS,
                List.of(),
                development,
                List.of(),
                0.0,
                false);
    }
}
