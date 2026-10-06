package co.surumene.www.wolftrap;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class WolfTrapStateStore {
    private static final byte ARMED = 1;
    private static final byte RIDER = 2;
    private static final byte MOUNTED_WOLF = 3;

    private final NamespacedKey stateKey;
    private final NamespacedKey graceUntilKey;
    private final NamespacedKey pairKey;

    WolfTrapStateStore(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        stateKey = new NamespacedKey(plugin, "wolf_trap_state");
        graceUntilKey =
                new NamespacedKey(plugin, "wolf_trap_grace_until");
        pairKey = new NamespacedKey(plugin, "wolf_trap_pair");
    }

    void markArmed(
            Evoker evoker,
            long graceUntilMillis) {
        PersistentDataContainer pdc =
                evoker.getPersistentDataContainer();
        pdc.set(stateKey, PersistentDataType.BYTE, ARMED);
        pdc.set(
                graceUntilKey,
                PersistentDataType.LONG,
                graceUntilMillis);
        pdc.remove(pairKey);
    }

    boolean isArmed(Evoker evoker) {
        return state(evoker.getPersistentDataContainer()) == ARMED;
    }

    long graceUntil(Evoker evoker) {
        Long value = evoker.getPersistentDataContainer().get(
                graceUntilKey,
                PersistentDataType.LONG);
        return value == null ? Long.MIN_VALUE : value;
    }

    void clearArmed(Evoker evoker) {
        clear(evoker.getPersistentDataContainer());
    }

    void markRider(
            Evoker evoker,
            UUID wolfId) {
        PersistentDataContainer pdc =
                evoker.getPersistentDataContainer();
        pdc.set(stateKey, PersistentDataType.BYTE, RIDER);
        pdc.set(
                pairKey,
                PersistentDataType.STRING,
                Objects.requireNonNull(wolfId, "wolfId").toString());
        pdc.remove(graceUntilKey);
    }

    boolean isRider(Evoker evoker) {
        return state(evoker.getPersistentDataContainer()) == RIDER;
    }

    Optional<UUID> riderWolfId(Evoker evoker) {
        return pair(evoker.getPersistentDataContainer());
    }

    void clearRider(Evoker evoker) {
        clear(evoker.getPersistentDataContainer());
    }

    void markMountedWolf(
            Wolf wolf,
            UUID riderId) {
        PersistentDataContainer pdc =
                wolf.getPersistentDataContainer();
        pdc.set(stateKey, PersistentDataType.BYTE, MOUNTED_WOLF);
        pdc.set(
                pairKey,
                PersistentDataType.STRING,
                Objects.requireNonNull(riderId, "riderId").toString());
        pdc.remove(graceUntilKey);
    }

    boolean isMountedWolf(Wolf wolf) {
        return state(wolf.getPersistentDataContainer())
                == MOUNTED_WOLF;
    }

    Optional<UUID> mountedRiderId(Wolf wolf) {
        return pair(wolf.getPersistentDataContainer());
    }

    void clearMountedWolf(Wolf wolf) {
        clear(wolf.getPersistentDataContainer());
    }

    private byte state(PersistentDataContainer pdc) {
        Byte value = pdc.get(stateKey, PersistentDataType.BYTE);
        return value == null ? 0 : value;
    }

    private Optional<UUID> pair(PersistentDataContainer pdc) {
        String value = pdc.get(pairKey, PersistentDataType.STRING);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private void clear(PersistentDataContainer pdc) {
        pdc.remove(stateKey);
        pdc.remove(graceUntilKey);
        pdc.remove(pairKey);
    }
}
