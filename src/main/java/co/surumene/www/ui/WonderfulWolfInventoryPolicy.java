package co.surumene.www.ui;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;

import java.util.Objects;

public final class WonderfulWolfInventoryPolicy {
    public static final int INVENTORY_SIZE = 54;
    public static final int MANAGEMENT_SIZE = 9;
    public static final int MAX_CARGO_SLOTS = 45;

    public static final int SLOT_WEAPON = 0;
    public static final int SLOT_ACTION_DISTANCE = 5;
    public static final int SLOT_MODE = 6;
    public static final int SLOT_INFO = 7;
    public static final int SLOT_PEDIGREE = 8;

    private WonderfulWolfInventoryPolicy() {}

    public static boolean isManagementSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < MANAGEMENT_SIZE;
    }

    public static boolean isCargoSlot(int rawSlot) {
        return rawSlot >= MANAGEMENT_SIZE && rawSlot < INVENTORY_SIZE;
    }

    public static boolean isUsableCargoSlot(int rawSlot, int capacity) {
        int bounded = Math.max(0, Math.min(MAX_CARGO_SLOTS, capacity));
        return isCargoSlot(rawSlot)
                && rawSlot - MANAGEMENT_SIZE < bounded;
    }

    public static int cargoIndex(int rawSlot) {
        if (!isCargoSlot(rawSlot)) {
            throw new IllegalArgumentException("rawSlot is not a cargo slot");
        }
        return rawSlot - MANAGEMENT_SIZE;
    }

    public static Mode cycleMode(Mode current, boolean forward) {
        Objects.requireNonNull(current, "current");
        Mode[] values = Mode.values();
        return values[Math.floorMod(current.ordinal() + (forward ? 1 : -1), values.length)];
    }

    public static ActionDistance cycleActionDistance(
            ActionDistance current,
            boolean forward) {
        Objects.requireNonNull(current, "current");
        ActionDistance[] values = ActionDistance.values();
        return values[Math.floorMod(current.ordinal() + (forward ? 1 : -1), values.length)];
    }
}
