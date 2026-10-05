package co.surumene.www.spawn;

import co.surumene.www.domain.*;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.individual.*;
import co.surumene.www.persistence.*;
import co.surumene.wgl.api.*;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

final class PaperWonderfulWolfFactoryTest {
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());

    @Test
    void adultTamedWolfKeepsVanillaStateAndPersistsMatchingFounderMetadata() {
        UUID ownerId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        OfflinePlayer owner = offlinePlayer(ownerId);
        MemoryPdc pdc = new MemoryPdc();
        Wolf wolf = wolf(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                pdc.proxy(),
                true,
                true,
                owner);
        WonderfulWolfLoadedIndividuals loaded = loaded(pdc);
        AtomicInteger refreshes = new AtomicInteger();

        PaperWonderfulWolfFactory factory = new PaperWonderfulWolfFactory(
                founderSource(),
                loaded,
                () -> 7654321L,
                ignored -> refreshes.incrementAndGet());

        WonderfulWolfEntityCreationResult.Success success = assertInstanceOf(
                WonderfulWolfEntityCreationResult.Success.class,
                factory.convertExisting(wolf, FounderOrigin.NATURAL, 1L));

        assertEquals(Optional.of(ownerId), success.individual().ownerId());
        assertEquals(7654321L, success.individual().adultBiologicalTime());
        assertEquals(success.individual(), loaded.find(wolf.getUniqueId()).orElseThrow());
        assertEquals(1, refreshes.get());
        assertTrue(pdc.keys().stream()
                .map(NamespacedKey::toString)
                .anyMatch("whatawonderfulwolf:type"::equals));
    }

    @Test
    void babyWolfLeavesAdultTimeUnsetAndDoesNotTriggerAdultAbilityProjection() {
        MemoryPdc pdc = new MemoryPdc();
        Wolf wolf = wolf(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                pdc.proxy(),
                false,
                false,
                null);
        WonderfulWolfLoadedIndividuals loaded = loaded(pdc);
        AtomicInteger refreshes = new AtomicInteger();

        PaperWonderfulWolfFactory factory = new PaperWonderfulWolfFactory(
                founderSource(),
                loaded,
                () -> 999999L,
                ignored -> refreshes.incrementAndGet());

        WonderfulWolfEntityCreationResult.Success success = assertInstanceOf(
                WonderfulWolfEntityCreationResult.Success.class,
                factory.convertExisting(wolf, FounderOrigin.NATURAL, 2L));

        assertTrue(success.individual().ownerId().isEmpty());
        assertEquals(0L, success.individual().adultBiologicalTime());
        assertEquals(0, refreshes.get());
    }

    @Test
    void convertedFounderRestoresThroughAFreshRegistryAfterEntityReplacement() {
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        MemoryPdc pdc = new MemoryPdc();
        Wolf original = wolf(id, pdc.proxy(), true, false, null);
        WonderfulWolfLoadedIndividuals firstRegistry = loaded(pdc);

        PaperWonderfulWolfFactory factory = new PaperWonderfulWolfFactory(
                founderSource(),
                firstRegistry,
                () -> 456789L,
                ignored -> {});
        WonderfulWolfEntityCreationResult.Success created = assertInstanceOf(
                WonderfulWolfEntityCreationResult.Success.class,
                factory.convertExisting(original, FounderOrigin.NATURAL, 4L));

        firstRegistry.clear();
        Wolf replacement = wolf(id, pdc.proxy(), true, false, null);
        WonderfulWolfLoadedIndividuals restartedRegistry = loaded(pdc);

        RestoreResult.Success restored = assertInstanceOf(
                RestoreResult.Success.class,
                restartedRegistry.register(replacement));
        assertEquals(created.individual(), restored.individual());
    }

    @Test
    void existingWonderfulWolfIsNotReplacedByAnotherFounder() {
        MemoryPdc pdc = new MemoryPdc();
        Wolf wolf = wolf(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                pdc.proxy(),
                true,
                false,
                null);
        WonderfulWolfLoadedIndividuals loaded = loaded(pdc);
        WonderfulWolfIndividual source = individual(Optional.empty(), 1234L);
        loaded.saveAndRegister(wolf, source);

        AtomicInteger generated = new AtomicInteger();
        FounderIndividualSource sourceFactory = (origin, owner, adultTime, seed) -> {
            generated.incrementAndGet();
            return new WonderfulWolfCreationResult.Success(
                    individual(owner, adultTime));
        };

        PaperWonderfulWolfFactory factory = new PaperWonderfulWolfFactory(
                sourceFactory,
                loaded,
                () -> 8888L,
                ignored -> {});

        WonderfulWolfEntityCreationResult.AlreadyWonderful existing = assertInstanceOf(
                WonderfulWolfEntityCreationResult.AlreadyWonderful.class,
                factory.convertExisting(wolf, FounderOrigin.NATURAL, 3L));

        assertEquals(source, existing.individual());
        assertEquals(0, generated.get());
    }

    private FounderIndividualSource founderSource() {
        return (origin, owner, adultTime, seed) ->
                new WonderfulWolfCreationResult.Success(
                        individual(owner, adultTime));
    }

    private WonderfulWolfLoadedIndividuals loaded(MemoryPdc pdc) {
        return new WonderfulWolfLoadedIndividuals(
                new WonderfulWolfEntityStore(plugin(), engine));
    }

    private static WonderfulWolfIndividual individual(
            Optional<UUID> owner,
            long adultTime) {
        return new WonderfulWolfIndividual(
                genome(),
                phenotype(),
                owner,
                adultTime,
                Mode.WANDER,
                Optional.empty(),
                ActionDistance.NORMAL,
                Optional.empty(),
                Map.of(),
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

    private static OfflinePlayer offlinePlayer(UUID id) {
        return (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "OfflinePlayer[" + id + "]";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Wolf wolf(
            UUID id,
            PersistentDataContainer pdc,
            boolean adult,
            boolean tamed,
            OfflinePlayer owner) {
        return (Wolf) Proxy.newProxyInstance(
                Wolf.class.getClassLoader(),
                new Class<?>[]{Wolf.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPersistentDataContainer" -> pdc;
                    case "getUniqueId" -> id;
                    case "isAdult" -> adult;
                    case "isTamed" -> tamed;
                    case "getOwner" -> owner;
                    case "setAdult", "setBaby", "setTamed", "setOwner" ->
                            throw new AssertionError(
                                    "Phase 7 conversion must preserve vanilla Wolf state: "
                                            + method.getName());
                    case "isValid" -> true;
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

        Set<NamespacedKey> keys() {
            return Set.copyOf(values.keySet());
        }

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
}
