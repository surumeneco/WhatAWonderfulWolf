package co.surumene.www.behavior;
import co.surumene.www.persistence.*;
import co.surumene.www.domain.Mode;
import org.bukkit.entity.Wolf;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityTameEvent;

public final class WonderfulWolfTameListener implements Listener {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final org.bukkit.plugin.Plugin plugin;
    public WonderfulWolfTameListener(org.bukkit.plugin.Plugin plugin, WonderfulWolfLoadedIndividuals loaded) {
        this.plugin = plugin;
        this.loaded = loaded;
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        RestoreResult result = loaded.register(wolf);
        if (!(result instanceof RestoreResult.Success success)) return;
        var previous = success.individual();
        loaded.saveAndRegister(wolf,
                previous.withTamedOwner(event.getOwner().getUniqueId()));
        if (previous.mode() == Mode.WANDER) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (wolf.isValid() && wolf.isTamed()) wolf.setSitting(false);
            });
        }
    }
}
