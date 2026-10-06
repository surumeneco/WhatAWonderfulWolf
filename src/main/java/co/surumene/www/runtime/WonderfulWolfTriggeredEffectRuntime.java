package co.surumene.www.runtime;

import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.individual.WonderfulWolfIndividual;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class WonderfulWolfTriggeredEffectRuntime {
    private static final int EFFECT_TICKS = 100;
    private static final long HOLY_PULSE_INTERVAL_TICKS = 20L;

    private final Server server;
    private final Map<UUID, HolyPulse> holyPulses = new HashMap<>();

    WonderfulWolfTriggeredEffectRuntime(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    void onHit(
            WonderfulWolfIndividual individual,
            LivingEntity target,
            long serverTick) {
        ExpressedTraitLookup.strength(individual, Trait.INTIMIDATION)
                .ifPresent(strength ->
                        WonderfulWolfStatusEffectRuntime.applyPotion(
                                target,
                                MinecraftTriggeredEffectTypes.forTrait(
                                        Trait.INTIMIDATION),
                                EFFECT_TICKS,
                                EffectPolicy.amplifier(
                                        Trait.INTIMIDATION,
                                        strength),
                                0));

        ExpressedTraitLookup.strength(individual, Trait.HOLY_POISON)
                .ifPresent(strength ->
                        applyHoly(target, strength, serverTick));
    }

    void onKill(
            WonderfulWolfIndividual individual,
            Wolf wolf) {
        ExpressedTraitLookup.strength(individual, Trait.LIFE_DRAIN)
                .ifPresent(strength ->
                        WonderfulWolfStatusEffectRuntime.applyInstantHealing(
                                wolf,
                                EffectPolicy.amplifier(
                                        Trait.LIFE_DRAIN,
                                        strength)));
    }

    void tick(long serverTick) {
        var iterator = holyPulses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HolyPulse> entry = iterator.next();
            Entity entity = server.getEntity(entry.getKey());
            HolyPulse pulse = entry.getValue();
            if (!(entity instanceof LivingEntity target)
                    || target.isDead()
                    || !target.isValid()
                    || serverTick >= pulse.expiresAt()) {
                iterator.remove();
                continue;
            }

            if (serverTick >= pulse.nextTick()) {
                WonderfulWolfStatusEffectRuntime.applyInstantHealing(
                        target,
                        pulse.amplifier());
                entry.setValue(new HolyPulse(
                        pulse.amplifier(),
                        pulse.nextTick() + HOLY_PULSE_INTERVAL_TICKS,
                        pulse.expiresAt()));
            }
        }
    }

    void clear() {
        holyPulses.clear();
    }

    private void applyHoly(
            LivingEntity target,
            TraitStrength strength,
            long serverTick) {
        int amplifier =
                EffectPolicy.amplifier(Trait.HOLY_POISON, strength);
        if (!Tag.ENTITY_TYPES_UNDEAD.isTagged(target.getType())) {
            WonderfulWolfStatusEffectRuntime.applyPotion(
                    target,
                    MinecraftTriggeredEffectTypes.forTrait(
                            Trait.HOLY_POISON),
                    EFFECT_TICKS,
                    amplifier,
                    0);
            return;
        }

        WonderfulWolfStatusEffectRuntime.applyInstantHealing(
                target,
                amplifier);
        holyPulses.merge(
                target.getUniqueId(),
                new HolyPulse(
                        amplifier,
                        serverTick + HOLY_PULSE_INTERVAL_TICKS,
                        serverTick + EFFECT_TICKS),
                (current, incoming) -> new HolyPulse(
                        Math.max(
                                current.amplifier(),
                                incoming.amplifier()),
                        Math.min(
                                current.nextTick(),
                                incoming.nextTick()),
                        Math.max(
                                current.expiresAt(),
                                incoming.expiresAt())));
    }

    private record HolyPulse(
            int amplifier,
            long nextTick,
            long expiresAt) {}
}
