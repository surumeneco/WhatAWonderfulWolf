package co.surumene.www.behavior;

import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.Mode;
import co.surumene.www.individual.WorldPosition;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.runtime.NonAttributeAbilityAdapter;
import co.surumene.www.runtime.WonderfulWolfAbilityRuntime;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class WonderfulWolfBehaviorRuntime {
    private static final double RETURN_DISTANCE_SQUARED = 4.0;
    private static final double WAIT_RETURN_DISTANCE_SQUARED = 1.0;
    private static final double FLEE_DISTANCE_BLOCKS = 8.0;

    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfAbilityRuntime abilities;
    private final Supplier<WwwConfig.Runtime> config;
    private final ManualTargetRegistry manualTargets;
    private final WonderfulWolfGoalAdapter goals;
    private final Server server;
    private final Logger logger;

    private final Map<UUID, UUID> selfAttackers = new HashMap<>();
    private final Map<UUID, UUID> commandCombat = new HashMap<>();
    private final Map<UUID, UUID> pvpIntervention = new HashMap<>();
    private final Map<UUID, RetreatState> retreat = new HashMap<>();
    private final Map<UUID, UUID> selectedTargets = new HashMap<>();

    public WonderfulWolfBehaviorRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfAbilityRuntime abilities,
            Supplier<WwwConfig.Runtime> config,
            ManualTargetRegistry manualTargets,
            WonderfulWolfGoalAdapter goals,
            Server server,
            Logger logger) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
        this.config = Objects.requireNonNull(config, "config");
        this.manualTargets = Objects.requireNonNull(manualTargets, "manualTargets");
        this.goals = Objects.requireNonNull(goals, "goals");
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void tick(long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must be >= 0");
        }

        WwwConfig.Runtime settings =
                Objects.requireNonNull(config.get(), "runtime config");
        Set<UUID> seen = new HashSet<>();

        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            Wolf wolf = snapshot.entity();
            UUID id = wolf.getUniqueId();
            seen.add(id);

            if (!wolf.isValid() || wolf.isDead()) {
                clearTransient(id);
                continue;
            }

            goals.prepare(wolf);
            try {
                evaluateWolf(
                        wolf,
                        snapshot.individual(),
                        settings,
                        serverTick);
            } catch (RuntimeException error) {
                logger.warning(
                        "Failed to update Wonderful Wolf behavior for "
                                + id
                                + ": "
                                + safeMessage(error));
            }
        }

        retain(seen);
        goals.retain(seen);
    }

    public void onCommandStateChanged(Wolf wolf) {
        Objects.requireNonNull(wolf, "wolf");
        UUID id = wolf.getUniqueId();
        manualTargets.clear(id);
        commandCombat.remove(id);
        pvpIntervention.remove(id);
        selectedTargets.remove(id);
        wolf.setTarget(null);
        tickWolfNow(wolf);
    }

    public boolean submitManualTarget(
            Wolf wolf,
            Player issuer,
            LivingEntity target,
            double draw) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(target, "target");

        WonderfulWolfIndividual individual =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (individual == null
                || individual.mode() == Mode.WANDER
                || target instanceof Player
                || target.equals(wolf)
                || target.isDead()
                || !target.isValid()) {
            return false;
        }

        return manualTargets.submit(
                individual,
                wolf.getUniqueId(),
                issuer.getUniqueId(),
                target.getUniqueId(),
                draw);
    }

    public void recordSelfAttacker(
            Wolf wolf,
            LivingEntity attacker) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(attacker, "attacker");
        if (!attacker.equals(wolf)) {
            selfAttackers.put(
                    wolf.getUniqueId(),
                    attacker.getUniqueId());
        }
    }

    public void recordCommanderCombat(
            Wolf wolf,
            LivingEntity target) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(target, "target");
        WonderfulWolfIndividual individual =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (individual == null
                || individual.mode() == Mode.WANDER
                || target.equals(wolf)) {
            return;
        }
        commandCombat.put(
                wolf.getUniqueId(),
                target.getUniqueId());
    }

    public void recordPvpConflict(
            Wolf wolf,
            Player first,
            Player second,
            double draw) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        WonderfulWolfIndividual individual =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (individual == null
                || individual.mode() == Mode.WANDER
                || individual.commanderId().isEmpty()) {
            return;
        }

        UUID commander = individual.commanderId().orElseThrow();
        if (!commander.equals(first.getUniqueId())
                && !commander.equals(second.getUniqueId())) {
            return;
        }

        java.util.LinkedHashMap<UUID, Long> weights =
                new java.util.LinkedHashMap<>();
        weights.put(
                first.getUniqueId(),
                AffectionPolicy.current(individual, first.getUniqueId()));
        weights.put(
                second.getUniqueId(),
                AffectionPolicy.current(individual, second.getUniqueId()));

        UUID supported =
                AffectionCompetitionResolver.choose(weights, draw);
        UUID target = supported.equals(first.getUniqueId())
                ? second.getUniqueId()
                : first.getUniqueId();
        pvpIntervention.put(wolf.getUniqueId(), target);
    }

    public boolean allowsVanillaTarget(
            Wolf wolf,
            LivingEntity target) {
        Objects.requireNonNull(wolf, "wolf");
        if (target == null) {
            return true;
        }
        UUID selected = selectedTargets.get(wolf.getUniqueId());
        return selected != null
                && selected.equals(target.getUniqueId());
    }

    public boolean isRetreating(UUID wolfId) {
        return retreat.getOrDefault(
                Objects.requireNonNull(wolfId, "wolfId"),
                RetreatState.inactive()).active();
    }

    public void clear() {
        selfAttackers.clear();
        commandCombat.clear();
        pvpIntervention.clear();
        retreat.clear();
        selectedTargets.clear();
        manualTargets.clearAll();
        goals.clear();
    }

    private void tickWolfNow(Wolf wolf) {
        WonderfulWolfIndividual individual =
                loaded.find(wolf.getUniqueId()).orElse(null);
        if (individual == null || !wolf.isValid() || wolf.isDead()) {
            return;
        }
        goals.prepare(wolf);
        evaluateWolf(
                wolf,
                individual,
                Objects.requireNonNull(config.get(), "runtime config"),
                server.getCurrentTick());
    }

    private void evaluateWolf(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            WwwConfig.Runtime settings,
            long serverTick) {
        UUID id = wolf.getUniqueId();

        double patience = abilities.find(id)
                .map(NonAttributeAbilityAdapter::patience)
                .orElseGet(() ->
                        individual.phenotypeSnapshot()
                                .abilities()
                                .get(Ability.PATIENCE));
        double healthFraction = healthFraction(wolf);

        RetreatState previous =
                retreat.getOrDefault(id, RetreatState.inactive());
        RetreatState next = RetreatPolicy.evaluate(
                previous,
                healthFraction,
                patience,
                serverTick,
                settings.combat().retreatMinTicks());
        retreat.put(id, next);

        Location reference =
                referenceLocation(individual);
        double radius = individual.actionDistance()
                .blocks(settings.actionDistance());

        if (next.active()) {
            if (!previous.active()) {
                manualTargets.clear(id);
                commandCombat.remove(id);
                pvpIntervention.remove(id);
            }
            selectedTargets.remove(id);
            if (wolf.getTarget() != null) {
                wolf.setTarget(null);
            }
            fleeOrReturn(wolf, individual, reference, radius);
            return;
        }

        if (previous.active()) {
            selfAttackers.remove(id);
        }

        List<TargetCandidate> candidates = new ArrayList<>();
        addManualCandidate(
                candidates, wolf, individual, reference, radius);
        addTransientCandidate(
                candidates,
                selfAttackers,
                wolf,
                individual,
                reference,
                radius,
                TargetSource.SELF_ATTACKER);
        addTransientCandidate(
                candidates,
                commandCombat,
                wolf,
                individual,
                reference,
                radius,
                TargetSource.COMMAND_COMBAT);
        addTransientCandidate(
                candidates,
                pvpIntervention,
                wolf,
                individual,
                reference,
                radius,
                TargetSource.PVP_INTERVENTION);

        if (candidates.isEmpty()
                && (individual.mode() == Mode.GUARD
                    || individual.mode() == Mode.WAIT)) {
            addActiveSearchCandidates(
                    candidates,
                    wolf,
                    individual,
                    reference,
                    radius);
        }

        Optional<TargetCandidate> selected =
                TargetSelectionPolicy.select(
                        individual.mode(),
                        false,
                        candidates);
        if (selected.isPresent()) {
            LivingEntity target =
                    livingEntity(selected.orElseThrow().targetId());
            if (target != null) {
                selectedTargets.put(id, target.getUniqueId());
                if (!target.equals(wolf.getTarget())) {
                    wolf.setTarget(target);
                }
                return;
            }
        }

        selectedTargets.remove(id);
        if (wolf.getTarget() != null) {
            wolf.setTarget(null);
        }
        returnToReference(wolf, individual, reference);
    }

    private void addManualCandidate(
            List<TargetCandidate> out,
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Location reference,
            double radius) {
        UUID wolfId = wolf.getUniqueId();
        ManualTargetRegistry.ManualTarget manual =
                manualTargets.get(wolfId).orElse(null);
        if (manual == null) {
            return;
        }
        LivingEntity target = livingEntity(manual.targetId());
        TargetCandidate candidate = candidate(
                wolf,
                individual,
                reference,
                radius,
                target,
                TargetSource.MANUAL);
        if (candidate == null
                || (individual.mode() != Mode.WANDER
                    && !candidate.withinActionDistance())) {
            manualTargets.clear(wolfId);
            return;
        }
        out.add(candidate);
    }

    private void addTransientCandidate(
            List<TargetCandidate> out,
            Map<UUID, UUID> sourceMap,
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Location reference,
            double radius,
            TargetSource source) {
        UUID wolfId = wolf.getUniqueId();
        UUID targetId = sourceMap.get(wolfId);
        if (targetId == null) {
            return;
        }
        LivingEntity target = livingEntity(targetId);
        TargetCandidate candidate = candidate(
                wolf,
                individual,
                reference,
                radius,
                target,
                source);
        if (candidate == null
                || (individual.mode() != Mode.WANDER
                    && !candidate.withinActionDistance())) {
            sourceMap.remove(wolfId);
            return;
        }
        out.add(candidate);
    }

    private TargetCandidate candidate(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Location reference,
            double radius,
            LivingEntity target,
            TargetSource source) {
        if (target == null
                || target.equals(wolf)
                || target.isDead()
                || !target.isValid()
                || !sameWorld(wolf.getLocation(), target.getLocation())) {
            return null;
        }

        double wolfDistance =
                wolf.getLocation().distanceSquared(target.getLocation());
        boolean within = individual.mode() == Mode.WANDER
                || (reference != null
                    && sameWorld(reference, target.getLocation())
                    && reference.distanceSquared(target.getLocation())
                        <= radius * radius);
        double referenceDistance =
                reference != null
                        && sameWorld(reference, target.getLocation())
                ? reference.distanceSquared(target.getLocation())
                : wolfDistance;

        return new TargetCandidate(
                target.getUniqueId(),
                source,
                within,
                wolfDistance,
                referenceDistance);
    }

    private void addActiveSearchCandidates(
            List<TargetCandidate> out,
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Location reference,
            double radius) {
        if (reference == null || reference.getWorld() == null) {
            return;
        }

        UUID commander = individual.commanderId().orElse(null);
        Set<UUID> commandWolves = sameCommandWolfIds(commander);
        for (Entity entity : reference.getWorld().getNearbyEntities(
                reference,
                radius,
                radius,
                radius)) {
            if (!(entity instanceof LivingEntity living)
                    || living.equals(wolf)
                    || living instanceof Player
                    || living.isDead()
                    || !living.isValid()
                    || reference.distanceSquared(living.getLocation())
                        > radius * radius
                    || !isActiveThreat(
                            living,
                            wolf,
                            commander,
                            commandWolves)) {
                continue;
            }

            TargetCandidate candidate = candidate(
                    wolf,
                    individual,
                    reference,
                    radius,
                    living,
                    TargetSource.ACTIVE_SEARCH);
            if (candidate != null) {
                out.add(candidate);
            }
        }
    }

    private boolean isActiveThreat(
            LivingEntity entity,
            Wolf wolf,
            UUID commander,
            Set<UUID> commandWolves) {
        if (entity instanceof Monster) {
            return true;
        }
        if (!(entity instanceof Mob mob)) {
            return false;
        }

        LivingEntity target = mob.getTarget();
        if (target == null) {
            return false;
        }
        UUID targetId = target.getUniqueId();
        return targetId.equals(wolf.getUniqueId())
                || (commander != null && targetId.equals(commander))
                || commandWolves.contains(targetId);
    }

    private Set<UUID> sameCommandWolfIds(UUID commander) {
        if (commander == null) {
            return Set.of();
        }
        Set<UUID> result = new HashSet<>();
        for (WonderfulWolfLoadedIndividuals.LoadedSnapshot snapshot :
                loaded.snapshots()) {
            if (snapshot.individual().commanderId()
                    .filter(commander::equals)
                    .isPresent()) {
                result.add(snapshot.entity().getUniqueId());
            }
        }
        return result;
    }

    private void fleeOrReturn(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Location reference,
            double radius) {
        Location current = wolf.getLocation();
        Vector away = new Vector();
        double scan = Math.max(FLEE_DISTANCE_BLOCKS, radius);

        for (Entity entity : wolf.getWorld().getNearbyEntities(
                current, scan, scan, scan)) {
            if (!(entity instanceof LivingEntity living)
                    || living.equals(wolf)
                    || !isDangerForRetreat(living, wolf)) {
                continue;
            }
            Vector delta = current.toVector()
                    .subtract(living.getLocation().toVector());
            if (delta.lengthSquared() > 1.0e-9) {
                away.add(delta.normalize());
            }
        }

        UUID attackerId = selfAttackers.get(wolf.getUniqueId());
        LivingEntity attacker =
                attackerId == null ? null : livingEntity(attackerId);
        if (attacker != null
                && sameWorld(current, attacker.getLocation())) {
            Vector delta = current.toVector()
                    .subtract(attacker.getLocation().toVector());
            if (delta.lengthSquared() > 1.0e-9) {
                away.add(delta.normalize());
            }
        }

        if (away.lengthSquared() > 1.0e-9) {
            Location destination = current.clone().add(
                    away.normalize().multiply(FLEE_DISTANCE_BLOCKS));
            wolf.getPathfinder().moveTo(destination, 1.2);
            return;
        }

        returnToReference(wolf, individual, reference);
    }

    private static boolean isDangerForRetreat(
            LivingEntity entity,
            Wolf wolf) {
        if (entity instanceof Monster) {
            return true;
        }
        return entity instanceof Mob mob
                && wolf.equals(mob.getTarget());
    }

    private void returnToReference(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Location reference) {
        if (individual.mode() == Mode.WANDER) {
            return;
        }
        if (wolf.isSitting()) {
            wolf.setSitting(false);
        }
        if (reference == null
                || !sameWorld(wolf.getLocation(), reference)) {
            wolf.getPathfinder().stopPathfinding();
            return;
        }

        double distance =
                wolf.getLocation().distanceSquared(reference);
        if (individual.mode() == Mode.WAIT) {
            if (distance > WAIT_RETURN_DISTANCE_SQUARED) {
                wolf.getPathfinder().moveTo(reference, 1.0);
            } else {
                wolf.getPathfinder().stopPathfinding();
            }
            return;
        }

        Player commander = individual.commanderId()
                .map(server::getPlayer)
                .orElse(null);
        if (commander != null
                && commander.isOnline()
                && distance > RETURN_DISTANCE_SQUARED) {
            wolf.getPathfinder().moveTo(commander, 1.0);
        } else if (distance <= RETURN_DISTANCE_SQUARED) {
            wolf.getPathfinder().stopPathfinding();
        }
    }

    private Location referenceLocation(
            WonderfulWolfIndividual individual) {
        if (individual.mode() == Mode.WANDER) {
            return null;
        }
        if (individual.mode() == Mode.WAIT) {
            WorldPosition wait =
                    individual.waitLocation().orElse(null);
            if (wait == null) {
                return null;
            }
            World world = server.getWorld(wait.worldId());
            return world == null
                    ? null
                    : new Location(
                            world,
                            wait.x(),
                            wait.y(),
                            wait.z());
        }

        Player commander = individual.commanderId()
                .map(server::getPlayer)
                .orElse(null);
        return commander == null || !commander.isOnline()
                ? null
                : commander.getLocation();
    }

    private LivingEntity livingEntity(UUID id) {
        Entity entity = server.getEntity(id);
        return entity instanceof LivingEntity living
                ? living
                : null;
    }

    private static double healthFraction(Wolf wolf) {
        AttributeInstance max =
                wolf.getAttribute(Attribute.MAX_HEALTH);
        double maximum =
                max == null ? Math.max(1.0, wolf.getHealth()) : max.getValue();
        if (!(maximum > 0.0) || !Double.isFinite(maximum)) {
            return 1.0;
        }
        return Math.max(
                0.0,
                Math.min(1.0, wolf.getHealth() / maximum));
    }

    private static boolean sameWorld(
            Location first,
            Location second) {
        return first.getWorld() != null
                && first.getWorld().equals(second.getWorld());
    }

    private void clearTransient(UUID id) {
        manualTargets.clear(id);
        selfAttackers.remove(id);
        commandCombat.remove(id);
        pvpIntervention.remove(id);
        retreat.remove(id);
        selectedTargets.remove(id);
    }

    private void retain(Set<UUID> seen) {
        selfAttackers.keySet().retainAll(seen);
        commandCombat.keySet().retainAll(seen);
        pvpIntervention.keySet().retainAll(seen);
        retreat.keySet().retainAll(seen);
        selectedTargets.keySet().retainAll(seen);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
