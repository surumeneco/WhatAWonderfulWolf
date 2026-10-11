package co.surumene.www.behavior;
import co.surumene.www.persistence.*;
import co.surumene.www.domain.Mode;
import org.bukkit.entity.Wolf;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityTameEvent;

public final class WonderfulWolfTameListener implements Listener {
    private final WonderfulWolfLoadedIndividuals loaded;
    public WonderfulWolfTameListener(WonderfulWolfLoadedIndividuals loaded) {
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
        if (previous.mode() == Mode.WANDER) wolf.setSitting(false);
    }
}
