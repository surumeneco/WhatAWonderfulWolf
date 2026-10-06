package co.surumene.www.wolftrap;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

final class WolfTrapStateStore {
    private static final byte ARMED = 1;
    private static final byte RIDER = 2;
    private static final byte MOUNTED_WOLF = 3;

    private final NamespacedKey stateKey;
    private final NamespacedKey graceUntilKey;

    WolfTrapStateStore(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        stateKey = new NamespacedKey(plugin, "wolf_trap_state");
        graceUntilKey =
                new NamespacedKey(plugin, "wolf_trap_grace_until");
    }

    void markArmed(
            Evoker evoker,
            long graceUntilFullTime) {
        PersistentDataContainer pdc =
                evoker.getPersistentDataContainer();
        pdc.set(stateKey, PersistentDataType.BYTE, ARMED);
        pdc.set(
                graceUntilKey,
                PersistentDataType.LONG,
                graceUntilFullTime);
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

    void markRider(Evoker evoker) {
        PersistentDataContainer pdc =
                evoker.getPersistentDataContainer();
        pdc.set(stateKey, PersistentDataType.BYTE, RIDER);
        pdc.remove(graceUntilKey);
    }

    boolean isRider(Evoker evoker) {
        return state(evoker.getPersistentDataContainer()) == RIDER;
    }

    void clearRider(Evoker evoker) {
        clear(evoker.getPersistentDataContainer());
    }

    void markMountedWolf(Wolf wolf) {
        PersistentDataContainer pdc =
                wolf.getPersistentDataContainer();
        pdc.set(stateKey, PersistentDataType.BYTE, MOUNTED_WOLF);
        pdc.remove(graceUntilKey);
    }

    boolean isMountedWolf(Wolf wolf) {
        return state(wolf.getPersistentDataContainer())
                == MOUNTED_WOLF;
    }

    void clearMountedWolf(Wolf wolf) {
        clear(wolf.getPersistentDataContainer());
    }

    private byte state(PersistentDataContainer pdc) {
        Byte value = pdc.get(stateKey, PersistentDataType.BYTE);
        return value == null ? 0 : value;
    }

    private void clear(PersistentDataContainer pdc) {
        pdc.remove(stateKey);
        pdc.remove(graceUntilKey);
    }
}
