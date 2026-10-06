package co.surumene.www.ui;

import co.surumene.www.individual.ItemStackSnapshot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class PaperItemStackCodec {
    private PaperItemStackCodec() {}

    public static Optional<ItemStackSnapshot> snapshot(ItemStack item) {
        if (item == null || item.isEmpty() || item.getType() == Material.AIR) {
            return Optional.empty();
        }
        return Optional.of(new ItemStackSnapshot(item.serializeAsBytes()));
    }

    public static ItemStack restore(ItemStackSnapshot snapshot) {
        return ItemStack.deserializeBytes(snapshot.bytes());
    }
}
