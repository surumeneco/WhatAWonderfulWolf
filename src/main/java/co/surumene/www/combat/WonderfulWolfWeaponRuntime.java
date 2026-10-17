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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class WonderfulWolfWeaponRuntime {
    private static final double MELEE_REACH_EXPANSION = 2.0;

    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfBehaviorRuntime behavior;
    private final Logger logger;
    private final Map<UUID, Long> lastAttackTick = new HashMap<>();
    private final Map<UUID, ChaseState> activeChases = new HashMap<>();

    private record ChaseState(UUID targetId, org.bukkit.Location targetLocation) {}

    private final Map<UUID, ItemStack> expectedWeapons = new HashMap<>();

    public WonderfulWolfWeaponRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfBehaviorRuntime behavior,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void tick(long serverTick) {
        // Idle wolves require no weapon work. Weapon changes sync via persistence hooks.
        java.util.Set<UUID> combatants = behavior.activeCombatWolfIds();
        if (!activeChases.isEmpty()) activeChases.keySet().retainAll(combatants);
        for (UUID id : combatants) {
            Wolf wolf = loaded.entity(id).orElse(null);
            if (wolf == null || !wolf.isValid() || wolf.isDead()) continue;
            try {
                LivingEntity target = behavior.selectedTarget(id).orElse(null);
                if (target == null || target.isDead() || !target.isValid()) {
                    activeChases.remove(id);
                    continue;
                }
                if (!meleeReach(wolf, target)) {
                    var pathfinder = wolf.getPathfinder();
                    var location = target.getLocation();
                    ChaseState previous = activeChases.get(id);
                    boolean changed = previous == null
                            || !previous.targetId().equals(target.getUniqueId());
                    double moved = changed || previous.targetLocation().getWorld() != location.getWorld()
                            ? Double.POSITIVE_INFINITY
                            : previous.targetLocation().distanceSquared(location);
                    if (NavigationRepathPolicy.replan(pathfinder.hasPath(), changed, moved)
                            && pathfinder.moveTo(target, 1.0)) {
                        activeChases.put(id, new ChaseState(target.getUniqueId(), location.clone()));
                    }
                    continue;
                }
                activeChases.remove(id);
                AttributeInstance attackSpeed = wolf.getAttribute(Attribute.ATTACK_SPEED);
                if (attackSpeed == null) continue;
                double attacksPerSecond = WonderfulWolfWeaponTiming.attackRate(attackSpeed.getValue());
                long cooldown = WonderfulWolfWeaponTiming.cooldownTicks(
                        attacksPerSecond);
                long previous = lastAttackTick.getOrDefault(id, Long.MIN_VALUE / 2);
                if (cooldown != Long.MAX_VALUE && serverTick - previous >= cooldown) {
                    wolf.attack(target);
                    lastAttackTick.put(id, serverTick);
                }
            } catch (RuntimeException error) {
                logger.warning("Failed to update Wonderful Wolf weapon runtime for "
                        + id + ": " + safeMessage(error));
            }
        }

    }

    /** Drop cooldown metadata when the loaded instance is unloaded. */
    public void untrack(UUID id) {
        lastAttackTick.remove(id);
        activeChases.remove(id);
        expectedWeapons.remove(id);
    }

    public void syncWeapon(Wolf wolf) {
        loaded.find(wolf.getUniqueId()).ifPresent(individual -> syncWeapon(wolf, individual));
    }

    public void clear() {
        lastAttackTick.clear();
        activeChases.clear();
        expectedWeapons.clear();
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
        expectedWeapons.put(wolf.getUniqueId(), expected);
        ensureWeapon(wolf);
    }

    private void ensureWeapon(Wolf wolf) {
        ItemStack expected = expectedWeapons.get(wolf.getUniqueId());
        if (expected == null) return;
        ItemStack current = wolf.getEquipment().getItemInMainHand();
        if (!same(current, expected)) {
            wolf.getEquipment().setItemInMainHand(expected.clone());
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
