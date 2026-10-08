package co.surumene.www.ui;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.Mode;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class WanWandListener implements Listener {
    private static final double RAY_SIZE = 0.5;

    private final WanWandService wand;
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfInventoryService inventories;
    private final WonderfulWolfBehaviorRuntime behavior;
    private final Supplier<WwwConfig.Runtime> config;
    private final DoubleSupplier draw;

    public WanWandListener(
            WanWandService wand,
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfInventoryService inventories,
            WonderfulWolfBehaviorRuntime behavior,
            Supplier<WwwConfig.Runtime> config,
            DoubleSupplier draw) {
        this.wand = Objects.requireNonNull(wand, "wand");
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.inventories = Objects.requireNonNull(inventories, "inventories");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.config = Objects.requireNonNull(config, "config");
        this.draw = Objects.requireNonNull(draw, "draw");
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack item = event.getHand() == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItemInMainHand();
        if (!wand.isWanWand(item)) return;
        if (!(event.getRightClicked() instanceof Wolf wolf)) return;

        event.setCancelled(true);
        if (wolf.isTamed() && loaded.find(wolf.getUniqueId()).isPresent()) {
            inventories.open(event.getPlayer(), wolf);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null || !wand.isWanWand(event.getItem())) return;
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;
        event.setCancelled(true);
        submitRaycast(event.getPlayer());
    }

    @EventHandler
    public void onDirectAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !wand.isWanWand(player.getInventory().getItemInMainHand())) {
            return;
        }
        event.setCancelled(true);
        submitRaycast(player);
    }

    private void submitRaycast(Player player) {
        WwwConfig.Runtime runtime = Objects.requireNonNull(config.get(), "runtime config");
        RayTraceResult result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                runtime.wanWand().targetMaxDistanceBlocks(),
                FluidCollisionMode.NEVER,
                true,
                RAY_SIZE,
                entity -> entity instanceof LivingEntity && !entity.equals(player),
                block -> block.getType().isOccluding());
        if (result == null
                || !(result.getHitEntity() instanceof LivingEntity target)
                || target instanceof Player) {
            return;
        }

        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot : loaded.snapshots()) {
            if (!canReceiveManualInstruction(snapshot.individual().mode())) {
                continue;
            }
            behavior.submitManualTarget(
                    snapshot.entity(),
                    player,
                    target,
                    draw.getAsDouble());
        }
    }

    static boolean canReceiveManualInstruction(Mode mode) {
        return Objects.requireNonNull(mode, "mode") != Mode.WANDER;
    }
}
