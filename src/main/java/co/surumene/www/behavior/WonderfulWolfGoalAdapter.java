package co.surumene.www.behavior;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WonderfulWolfGoalAdapter {
    private final MobGoals goals;
    private final Map<UUID, Wolf> prepared = new java.util.HashMap<>();

    public WonderfulWolfGoalAdapter(MobGoals goals) {
        this.goals = Objects.requireNonNull(goals, "goals");
    }

    public void prepare(Wolf wolf) {
        Objects.requireNonNull(wolf, "wolf");
        Wolf current = prepared.get(wolf.getUniqueId());
        if (current == wolf) return;

        Tameable tameable = wolf;
        goals.removeGoal(tameable, VanillaGoal.FOLLOW_OWNER);
        goals.removeGoal(tameable, VanillaGoal.OWNER_HURT);
        goals.removeGoal(tameable, VanillaGoal.OWNER_HURT_BY);
        goals.removeGoal(wolf, VanillaGoal.MELEE_ATTACK);
        prepared.put(wolf.getUniqueId(), wolf);
    }

    public void retain(Iterable<UUID> loadedIds) {
        java.util.HashSet<UUID> ids = new java.util.HashSet<>();
        loadedIds.forEach(ids::add);
        prepared.keySet().retainAll(ids);
    }

    public void clear() {
        prepared.clear();
    }
}
