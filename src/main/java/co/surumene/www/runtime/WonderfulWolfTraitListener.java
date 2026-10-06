package co.surumene.www.runtime;

import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Objects;

public final class WonderfulWolfTraitListener implements Listener {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfTraitRuntime runtime;

    public WonderfulWolfTraitListener(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfTraitRuntime runtime) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)
                || loaded.find(wolf.getUniqueId()).isEmpty()) {
            return;
        }

        EffectPolicy.DamageKind kind = switch (event.getCause()) {
            case FALL -> EffectPolicy.DamageKind.FALL;
            case DROWNING -> EffectPolicy.DamageKind.DROWNING;
            case FREEZE -> EffectPolicy.DamageKind.FREEZING;
            default -> EffectPolicy.DamageKind.OTHER;
        };
        if (kind != EffectPolicy.DamageKind.OTHER
                && runtime.isImmune(wolf, kind)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Wolf wolf)
                || loaded.find(wolf.getUniqueId()).isEmpty()
                || !(event.getEntity() instanceof LivingEntity target)
                || event.getFinalDamage() <= 0.0) {
            return;
        }
        runtime.onHit(
                wolf,
                target,
                Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Entity causing =
                event.getDamageSource().getCausingEntity();
        if (causing instanceof Wolf wolf
                && loaded.find(wolf.getUniqueId()).isPresent()) {
            runtime.onKill(wolf);
        }
    }
}
