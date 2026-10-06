package co.surumene.www.combat;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.ui.PaperItemStackCodec;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public final class WonderfulWolfWeaponRuntime {
    private static final double MELEE_REACH_EXPANSION = 2.0;

    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfBehaviorRuntime behavior;
    private final Logger logger;
    private final Map<UUID, Long> lastAttackTick = new HashMap<>();

    public WonderfulWolfWeaponRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfBehaviorRuntime behavior,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void tick(long serverTick) {
        Set<UUID> seen = new HashSet<>();
        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot : loaded.snapshots()) {
            Wolf wolf = snapshot.entity();
            UUID id = wolf.getUniqueId();
            seen.add(id);
            if (!wolf.isValid() || wolf.isDead()) continue;
            try {
                syncWeapon(wolf, snapshot.individual());
                LivingEntity target = behavior.selectedTarget(id).orElse(null);
                if (target == null || target.isDead() || !target.isValid()) continue;
                if (!meleeReach(wolf, target)) {
                    wolf.getPathfinder().moveTo(target, 1.0);
                    continue;
                }
                AttributeInstance attackSpeed = wolf.getAttribute(Attribute.ATTACK_SPEED);
                double attacksPerSecond = attackSpeed == null ? 0.0 : attackSpeed.getValue();
                long cooldown = WonderfulWolfWeaponTiming.cooldownTicks(
                        Math.max(0.0, attacksPerSecond));
                long previous = lastAttackTick.getOrDefault(id, Long.MIN_VALUE / 2);
                if (cooldown != Long.MAX_VALUE && serverTick - previous >= cooldown) {
                    wolf.attack(target);
                    lastAttackTick.put(id, serverTick);
                    syncWeapon(wolf, snapshot.individual());
                }
            } catch (RuntimeException error) {
                logger.warning("Failed to update Wonderful Wolf weapon runtime for "
                        + id + ": " + safeMessage(error));
            }
        }
        lastAttackTick.keySet().retainAll(seen);
    }

    public void syncWeapon(Wolf wolf) {
        loaded.find(wolf.getUniqueId()).ifPresent(individual -> syncWeapon(wolf, individual));
    }

    public void clear() {
        lastAttackTick.clear();
    }

    private void syncWeapon(Wolf wolf, WonderfulWolfIndividual individual) {
        ItemStack expected = individual.weapon()
                .map(PaperItemStackCodec::restore)
                .orElseGet(() -> ItemStack.of(Material.AIR));
        if (!expected.isEmpty()) {
            ItemMeta meta = expected.getItemMeta();
            meta.setUnbreakable(true);
            expected.setItemMeta(meta);
        }
        ItemStack current = wolf.getEquipment().getItemInMainHand();
        if (!same(current, expected)) {
            wolf.getEquipment().setItemInMainHand(expected);
        }
    }

    private static boolean meleeReach(Wolf wolf, LivingEntity target) {
        return wolf.getBoundingBox()
                .expand(MELEE_REACH_EXPANSION)
                .overlaps(target.getBoundingBox());
    }

    private static boolean same(ItemStack first, ItemStack second) {
        boolean aEmpty = first == null || first.isEmpty();
        boolean bEmpty = second == null || second.isEmpty();
        if (aEmpty || bEmpty) return aEmpty == bEmpty;
        return first.getAmount() == second.getAmount() && first.isSimilar(second);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
