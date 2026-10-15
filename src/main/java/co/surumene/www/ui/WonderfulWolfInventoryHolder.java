package co.surumene.www.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;
import java.util.UUID;

public final class WonderfulWolfInventoryHolder implements InventoryHolder {
    private final UUID wolfId;
    private Inventory inventory;

    public WonderfulWolfInventoryHolder(UUID wolfId) {
        this.wolfId = Objects.requireNonNull(wolfId, "wolfId");
    }

    public UUID wolfId() {
        return wolfId;
    }

    public void inventory(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
