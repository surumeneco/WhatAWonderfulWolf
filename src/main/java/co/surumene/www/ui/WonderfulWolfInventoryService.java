package co.surumene.www.ui;

import co.surumene.www.ability.AbilityScale;
import co.surumene.www.behavior.WonderfulWolfCommandService;
import co.surumene.www.combat.WonderfulWolfWeaponRuntime;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.ItemStackSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.runtime.WonderfulWolfAbilityRuntime;
import co.surumene.wgl.api.GenomeEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class WonderfulWolfInventoryService {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfAbilityRuntime abilities;
    private final WonderfulWolfCommandService commands;
    private final WonderfulWolfWeaponRuntime weapons;
    private final GenomeEngine engine;
    private final Supplier<WonderfulWolfGenomeProfile> profile;
    private final WonderfulWolfPlayerInfo info = new WonderfulWolfPlayerInfo();
    private final Map<UUID, UUID> locks = new HashMap<>();

    public WonderfulWolfInventoryService(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfAbilityRuntime abilities,
            WonderfulWolfCommandService commands,
            WonderfulWolfWeaponRuntime weapons,
            GenomeEngine engine,
            Supplier<WonderfulWolfGenomeProfile> profile) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
        this.commands = Objects.requireNonNull(commands, "commands");
        this.weapons = Objects.requireNonNull(weapons, "weapons");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public void open(Player player, Wolf wolf) {
        WonderfulWolfIndividual individual = loaded.find(wolf.getUniqueId()).orElse(null);
        if (individual == null || !wolf.isTamed()) return;

        UUID viewer = locks.get(wolf.getUniqueId());
        if (viewer != null && !viewer.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("このWonderful Wolfは他のプレイヤーが操作中です。", NamedTextColor.RED));
            return;
        }

        WonderfulWolfInventoryHolder holder =
                new WonderfulWolfInventoryHolder(wolf.getUniqueId());
        Inventory inventory = Bukkit.createInventory(
                holder,
                WonderfulWolfInventoryPolicy.INVENTORY_SIZE,
                Component.text("Wonderful Wolf — " + wolf.getName()));
        holder.inventory(inventory);
        populate(inventory, wolf, individual);
        locks.put(wolf.getUniqueId(), player.getUniqueId());
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof WonderfulWolfInventoryHolder holder)) return;
        Entity entity = Bukkit.getEntity(holder.wolfId());
        if (!(entity instanceof Wolf wolf) || loaded.find(holder.wolfId()).isEmpty()) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            return;
        }

        int raw = event.getRawSlot();
        boolean topClicked = raw >= 0 && raw < top.getSize();

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        int capacity = capacity(wolf);
        if (topClicked && WonderfulWolfInventoryPolicy.isManagementSlot(raw)) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
            boolean forward = event.getClick() == ClickType.LEFT;
            switch (raw) {
                case WonderfulWolfInventoryPolicy.SLOT_WEAPON ->
                        handleWeaponClick(player, wolf, top);
                case WonderfulWolfInventoryPolicy.SLOT_ACTION_DISTANCE ->
                        changeActionDistance(player, wolf, top, forward);
                case WonderfulWolfInventoryPolicy.SLOT_MODE ->
                        changeMode(player, wolf, top, forward);
                case WonderfulWolfInventoryPolicy.SLOT_INFO ->
                        sendInfo(player, wolf, false);
                case WonderfulWolfInventoryPolicy.SLOT_PEDIGREE ->
                        sendInfo(player, wolf, true);
                default -> {
                }
            }
            return;
        }

        if (topClicked && WonderfulWolfInventoryPolicy.isCargoSlot(raw)
                && !WonderfulWolfInventoryPolicy.isUsableCargoSlot(raw, capacity)) {
            event.setCancelled(true);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof WonderfulWolfInventoryHolder holder)) return;
        Entity entity = Bukkit.getEntity(holder.wolfId());
        int capacity = entity instanceof Wolf wolf ? capacity(wolf) : 0;
        for (int raw : event.getRawSlots()) {
            if (raw < top.getSize()
                    && (!WonderfulWolfInventoryPolicy.isCargoSlot(raw)
                    || !WonderfulWolfInventoryPolicy.isUsableCargoSlot(raw, capacity))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof WonderfulWolfInventoryHolder holder)) return;
        Entity entity = Bukkit.getEntity(holder.wolfId());
        if (entity instanceof Wolf wolf) {
            persistCargo(wolf, top);
        }
        locks.remove(holder.wolfId(), event.getPlayer().getUniqueId());
    }

    public void closeAll() {
        for (UUID viewerId : java.util.List.copyOf(locks.values())) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null
                    && viewer.getOpenInventory().getTopInventory().getHolder()
                    instanceof WonderfulWolfInventoryHolder) {
                viewer.closeInventory();
            }
        }
        locks.clear();
    }

    private void handleWeaponClick(Player player, Wolf wolf, Inventory top) {
        WonderfulWolfIndividual current = loaded.find(wolf.getUniqueId()).orElse(null);
        if (current == null) return;

        ItemStack cursor = player.getItemOnCursor();
        boolean cursorEmpty = cursor == null || cursor.isEmpty();
        ItemStack previous = current.weapon().map(PaperItemStackCodec::restore).orElse(null);
        Optional<ItemStackSnapshot> replacement = Optional.empty();
        if (!cursorEmpty) {
            ItemStack one = cursor.clone();
            one.setAmount(1);
            replacement = PaperItemStackCodec.snapshot(one);
        }

        WonderfulWolfIndividual updated =
                current.withStorage(replacement, current.inventory());
        loaded.saveAndRegister(wolf, updated);
        weapons.syncWeapon(wolf);

        if (cursorEmpty || cursor.getAmount() == 1) {
            player.setItemOnCursor(previous == null ? ItemStack.of(Material.AIR) : previous);
        } else {
            ItemStack remainder = cursor.clone();
            remainder.setAmount(cursor.getAmount() - 1);
            player.setItemOnCursor(remainder);
            if (previous != null && !previous.isEmpty()) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(previous);
                leftovers.values().forEach(item ->
                        wolf.getWorld().dropItemNaturally(wolf.getLocation(), item));
            }
        }
        populateManagement(top, wolf, updated);
    }

    private void changeMode(
            Player player,
            Wolf wolf,
            Inventory top,
            boolean forward) {
        WonderfulWolfIndividual current = loaded.find(wolf.getUniqueId()).orElse(null);
        if (current == null) return;
        Mode next = WonderfulWolfInventoryPolicy.cycleMode(current.mode(), forward);
        if (commands.changeMode(wolf, next, player.getUniqueId())) {
            loaded.find(wolf.getUniqueId()).ifPresent(value ->
                    populateManagement(top, wolf, value));
        }
    }

    private void changeActionDistance(
            Player player,
            Wolf wolf,
            Inventory top,
            boolean forward) {
        WonderfulWolfIndividual current = loaded.find(wolf.getUniqueId()).orElse(null);
        if (current == null) return;
        ActionDistance next = WonderfulWolfInventoryPolicy.cycleActionDistance(
                current.actionDistance(), forward);
        if (commands.changeActionDistance(wolf, next)) {
            loaded.find(wolf.getUniqueId()).ifPresent(value ->
                    populateManagement(top, wolf, value));
        }
    }

    private void sendInfo(Player player, Wolf wolf, boolean pedigree) {
        WonderfulWolfIndividual individual = loaded.find(wolf.getUniqueId()).orElse(null);
        if (individual == null) return;
        if (pedigree) {
            info.sendPedigree(player, wolf, individual, lineageId(individual));
        } else {
            info.sendInfo(player, wolf, individual, abilities.find(wolf.getUniqueId()));
        }
    }

    private void populate(
            Inventory inventory,
            Wolf wolf,
            WonderfulWolfIndividual individual) {
        populateManagement(inventory, wolf, individual);
        int capacity = capacity(wolf);
        for (int cargo = 0; cargo < WonderfulWolfInventoryPolicy.MAX_CARGO_SLOTS; cargo++) {
            int raw = WonderfulWolfInventoryPolicy.MANAGEMENT_SIZE + cargo;
            if (cargo >= capacity) {
                inventory.setItem(raw, placeholder(
                        Material.BLACK_STAINED_GLASS_PANE, "使用不可"));
                continue;
            }
            ItemStack item = individual.inventory().containsKey(cargo)
                    ? PaperItemStackCodec.restore(individual.inventory().get(cargo))
                    : ItemStack.of(Material.AIR);
            inventory.setItem(raw, item);
        }
    }

    private void populateManagement(
            Inventory inventory,
            Wolf wolf,
            WonderfulWolfIndividual individual) {
        inventory.setItem(
                WonderfulWolfInventoryPolicy.SLOT_WEAPON,
                individual.weapon().map(PaperItemStackCodec::restore)
                        .orElseGet(() -> placeholder(
                                Material.WHITE_STAINED_GLASS_PANE, "武器")));
        for (int slot = 1; slot <= 4; slot++) {
            inventory.setItem(slot, placeholder(
                    Material.BLACK_STAINED_GLASS_PANE, "予約枠"));
        }
        inventory.setItem(
                WonderfulWolfInventoryPolicy.SLOT_ACTION_DISTANCE,
                named(Material.COMPASS,
                        "行動距離: " + individual.actionDistance().displayName()));
        inventory.setItem(
                WonderfulWolfInventoryPolicy.SLOT_MODE,
                named(modeMaterial(individual.mode()),
                        "モード: " + individual.mode().displayName()));
        inventory.setItem(
                WonderfulWolfInventoryPolicy.SLOT_INFO,
                info.infoItem(wolf, individual, abilities.find(wolf.getUniqueId())));
        inventory.setItem(
                WonderfulWolfInventoryPolicy.SLOT_PEDIGREE,
                info.pedigreeItem(individual, lineageId(individual)));
    }

    private void persistCargo(Wolf wolf, Inventory inventory) {
        WonderfulWolfIndividual current = loaded.find(wolf.getUniqueId()).orElse(null);
        if (current == null) return;
        int capacity = capacity(wolf);
        Map<Integer, ItemStackSnapshot> cargo = new HashMap<>(current.inventory());
        for (int i = 0; i < capacity; i++) {
            cargo.remove(i);
            PaperItemStackCodec.snapshot(
                    inventory.getItem(WonderfulWolfInventoryPolicy.MANAGEMENT_SIZE + i))
                    .ifPresent(snapshot -> cargo.put(i, snapshot));
        }
        loaded.saveAndRegister(
                wolf,
                current.withStorage(current.weapon(), cargo));
    }

    private int capacity(Wolf wolf) {
        double value = abilities.find(wolf.getUniqueId())
                .map(effective -> effective.canonical(Ability.INVENTORY))
                .orElseGet(() -> loaded.find(wolf.getUniqueId())
                        .map(individual -> AbilityScale.finalizeEffective(
                                Ability.INVENTORY,
                                AbilityScale.toCanonical(
                                        Ability.INVENTORY,
                                        individual.phenotypeSnapshot()
                                                .abilities().get(Ability.INVENTORY))))
                        .orElse(0.0));
        return Math.max(0, Math.min(
                WonderfulWolfInventoryPolicy.MAX_CARGO_SLOTS,
                (int) Math.round(value)));
    }

    private String lineageId(WonderfulWolfIndividual individual) {
        WonderfulWolfGenomeProfile currentProfile =
                Objects.requireNonNull(profile.get(), "current profile");
        return engine.marker(
                currentProfile.backbone(),
                individual.genome()).formatted();
    }

    private static Material modeMaterial(Mode mode) {
        return switch (mode) {
            case WANDER -> Material.FEATHER;
            case FOLLOW -> Material.LEAD;
            case GUARD -> Material.SHIELD;
            case WAIT -> Material.OAK_FENCE;
        };
    }

    private static ItemStack placeholder(Material material, String name) {
        return named(material, name);
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = ItemStack.of(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }
}
