package co.surumene.www.wolftrap;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.Objects;

public final class WolfTrapListener implements Listener {
    private final WolfTrapRuntime runtime;

    public WolfTrapListener(WolfTrapRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLightning(LightningStrikeEvent event) {
        runtime.onLightning(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Evoker evoker)
                || !playerAttack(event.getDamager())) {
            return;
        }
        runtime.activateByAttack(evoker);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Evoker evoker) {
            runtime.onEvokerDeath(evoker);
        } else if (event.getEntity() instanceof Wolf wolf) {
            runtime.onWolfDeath(wolf);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        runtime.markTimeSkip(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            runtime.register(entity);
        }
    }

    private static boolean playerAttack(Entity damager) {
        if (damager instanceof Player) {
            return true;
        }
        return damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player;
    }
}
