package co.surumene.www.behavior;

import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;
import java.util.UUID;
import java.util.function.DoubleSupplier;

public final class WonderfulWolfCombatListener implements Listener {
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfBehaviorRuntime behavior;
    private final DoubleSupplier competitionDraw;

    public WonderfulWolfCombatListener(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfBehaviorRuntime behavior,
            DoubleSupplier competitionDraw) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.competitionDraw =
                Objects.requireNonNull(competitionDraw, "competitionDraw");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0) {
            return;
        }

        LivingEntity attacker = attacker(event.getDamager());

        if (event.getEntity() instanceof Wolf wolf
                && loaded.find(wolf.getUniqueId()).isPresent()
                && attacker != null) {
            behavior.recordSelfAttacker(wolf, attacker);
        }

        if (!(event.getEntity() instanceof LivingEntity victim)
                || attacker == null) {
            return;
        }

        if (attacker instanceof Player first
                && victim instanceof Player second) {
            recordPvp(first, second);
            return;
        }

        if (attacker instanceof Player commander) {
            recordCommanderCombat(commander.getUniqueId(), victim);
        }
        if (victim instanceof Player commander) {
            recordCommanderCombat(commander.getUniqueId(), attacker);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)
                || loaded.find(wolf.getUniqueId()).isEmpty()) {
            return;
        }
        if (!behavior.allowsVanillaTarget(
                wolf,
                event.getTarget())) {
            event.setCancelled(true);
        }
    }

    private void recordPvp(
            Player first,
            Player second) {
        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            if (snapshot.individual().commanderId().isEmpty()) {
                continue;
            }
            UUID commander =
                    snapshot.individual().commanderId().orElseThrow();
            if (!commander.equals(first.getUniqueId())
                    && !commander.equals(second.getUniqueId())) {
                continue;
            }
            behavior.recordPvpConflict(
                    snapshot.entity(),
                    first,
                    second,
                    competitionDraw.getAsDouble());
        }
    }

    private void recordCommanderCombat(
            UUID commanderId,
            LivingEntity target) {
        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            if (snapshot.individual().commanderId()
                    .filter(commanderId::equals)
                    .isPresent()) {
                behavior.recordCommanderCombat(
                        snapshot.entity(),
                        target);
            }
        }
    }

    private static LivingEntity attacker(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}
