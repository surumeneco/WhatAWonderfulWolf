package co.surumene.www.runtime;

import co.surumene.www.individual.ItemStackSnapshot;
import co.surumene.www.ui.PaperItemStackCodec;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

final class WonderfulWolfCargoStore {
    private WonderfulWolfCargoStore() {}

    static boolean hasEmptySlot(
            Map<Integer, ItemStackSnapshot> cargo,
            int capacity) {
        for (int i = 0; i < capacity; i++) {
            if (!cargo.containsKey(i)) {
                return true;
            }
        }
        return false;
    }

    static int store(
            Map<Integer, ItemStackSnapshot> cargo,
            int capacity,
            ItemStack input) {
        int remaining = input.getAmount();

        for (int i = 0; i < capacity && remaining > 0; i++) {
            ItemStackSnapshot snapshot = cargo.get(i);
            if (snapshot == null) {
                continue;
            }
            ItemStack existing = PaperItemStackCodec.restore(snapshot);
            if (!existing.isSimilar(input)
                    || existing.getAmount() >= existing.getMaxStackSize()) {
                continue;
            }
            int accepted = Math.min(
                    remaining,
                    existing.getMaxStackSize() - existing.getAmount());
            existing.setAmount(existing.getAmount() + accepted);
            cargo.put(
                    i,
                    PaperItemStackCodec.snapshot(existing).orElseThrow());
            remaining -= accepted;
        }

        for (int i = 0; i < capacity && remaining > 0; i++) {
            if (cargo.containsKey(i)) {
                continue;
            }
            ItemStack inserted = input.clone();
            int accepted =
                    Math.min(remaining, inserted.getMaxStackSize());
            inserted.setAmount(accepted);
            cargo.put(
                    i,
                    PaperItemStackCodec.snapshot(inserted).orElseThrow());
            remaining -= accepted;
        }
        return remaining;
    }
}
