package co.surumene.www.behavior;

import co.surumene.www.persistence.WonderfulWolfEntityStore;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class BehaviorPaperTestSupport {
    private BehaviorPaperTestSupport() {}

    static WonderfulWolfLoadedIndividuals loaded() {
        return new WonderfulWolfLoadedIndividuals(
                new WonderfulWolfEntityStore(
                        plugin(),
                        WonderfulGenomeEngine.create(EngineConfig.defaults())));
    }

    static Plugin plugin() {
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

    static World world(UUID id) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUID" -> id;
                    case "getName" -> "world";
                    case "toString" -> "World[" + id + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    static WolfFixture wolf(
            UUID id,
            World world,
            MemoryPdc pdc,
            Location location,
            boolean sitting) {
        AtomicBoolean sittingState = new AtomicBoolean(sitting);
        AtomicInteger stopCalls = new AtomicInteger();

        Pathfinder pathfinder = (Pathfinder) Proxy.newProxyInstance(
                Pathfinder.class.getClassLoader(),
                new Class<?>[]{Pathfinder.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "stopPathfinding" -> {
                        stopCalls.incrementAndGet();
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "Pathfinder";
                    default -> defaultValue(method.getReturnType());
                });

        Wolf wolf = (Wolf) Proxy.newProxyInstance(
                Wolf.class.getClassLoader(),
                new Class<?>[]{Wolf.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPersistentDataContainer" -> pdc.proxy();
                    case "getUniqueId" -> id;
                    case "getWorld" -> world;
                    case "getLocation" -> location.clone();
                    case "isSitting" -> sittingState.get();
                    case "setSitting" -> {
                        sittingState.set((Boolean) args[0]);
                        yield null;
                    }
                    case "setTarget" -> null;
                    case "getPathfinder" -> pathfinder;
                    case "isValid" -> true;
                    case "isDead" -> false;
                    case "toString" -> "Wolf[" + id + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });

        return new WolfFixture(
                wolf,
                sittingState,
                stopCalls);
    }

    static Object defaultValue(Class<?> type) {
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

    record WolfFixture(
            Wolf wolf,
            AtomicBoolean sitting,
            AtomicInteger stopCalls) {}

    static final class MemoryPdc {
        private final Map<NamespacedKey, Object> values = new HashMap<>();

        PersistentDataContainer proxy() {
            return (PersistentDataContainer) Proxy.newProxyInstance(
                    PersistentDataContainer.class.getClassLoader(),
                    new Class<?>[]{PersistentDataContainer.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "get" -> values.get((NamespacedKey) args[0]);
                        case "set" -> {
                            Object value = args[2];
                            if (value instanceof byte[] bytes) {
                                value = bytes.clone();
                            }
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
