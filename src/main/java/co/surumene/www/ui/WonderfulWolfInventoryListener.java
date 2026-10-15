package co.surumene.www.ui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Objects;

public final class WonderfulWolfInventoryListener implements Listener {
    private final WonderfulWolfInventoryService inventories;

    public WonderfulWolfInventoryListener(WonderfulWolfInventoryService inventories) {
        this.inventories = Objects.requireNonNull(inventories, "inventories");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        inventories.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        inventories.handleDrag(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        inventories.handleClose(event);
    }
}
