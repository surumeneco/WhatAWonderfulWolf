package co.surumene.www.runtime;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.individual.WonderfulWolfIndividual;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

final class WonderfulWolfStatusEffectRuntime {
    private static final int EFFECT_TICKS = 80;
    private static final int REFRESH_THRESHOLD = 40;

    void tick(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            WwwConfig.Runtime settings) {
        applySelfEffects(wolf, individual);
        applyAuras(wolf, individual, settings.traits().auraRadiusBlocks());
    }

    static void applyPotion(
            LivingEntity entity,
            PotionEffectType type,
            int durationTicks,
            int amplifier,
            int refreshThresholdTicks) {
        PotionEffect current = entity.getPotionEffect(type);
        if (current != null
                && !EffectPolicy.shouldApplyPotion(
                        current.getAmplifier(),
                        current.getDuration(),
                        amplifier,
                        refreshThresholdTicks)) {
            return;
        }
        entity.addPotionEffect(new PotionEffect(
                type,
                durationTicks,
                amplifier,
                true,
                false,
                false));
    }

    static void applyInstantHealing(
            LivingEntity entity,
            int amplifier) {
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.INSTANT_HEALTH,
                1,
                amplifier,
                true,
                false,
                false));
    }

    private void applySelfEffects(
            Wolf wolf,
            WonderfulWolfIndividual individual) {
        if (ExpressedTraitLookup.has(individual, Trait.FIRE_BIRD)) {
            applyPotion(
                    wolf,
                    PotionEffectType.FIRE_RESISTANCE,
                    EFFECT_TICKS,
                    0,
                    REFRESH_THRESHOLD);
        }

        ExpressedTraitLookup.strength(individual, Trait.UNYIELDING)
                .ifPresent(strength -> applyPotion(
                        wolf,
                        PotionEffectType.REGENERATION,
                        EFFECT_TICKS,
                        EffectPolicy.amplifier(
                                Trait.UNYIELDING,
                                strength),
                        REFRESH_THRESHOLD));

        if (ExpressedTraitLookup.has(individual, Trait.SWIMMER)) {
            applyPotion(
                    wolf,
                    PotionEffectType.WATER_BREATHING,
                    EFFECT_TICKS,
                    0,
                    REFRESH_THRESHOLD);
        }
    }

    private void applyAuras(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            double radius) {
        if (!(radius > 0.0)) {
            return;
        }
        applyAura(
                wolf,
                individual,
                Trait.MINING_SUPPORT,
                PotionEffectType.HASTE,
                radius);
        applyAura(
                wolf,
                individual,
                Trait.MUSCLE,
                PotionEffectType.STRENGTH,
                radius);
        applyAura(
                wolf,
                individual,
                Trait.GUARDIAN,
                PotionEffectType.RESISTANCE,
                radius);
        applyAura(
                wolf,
                individual,
                Trait.HEALING,
                PotionEffectType.REGENERATION,
                radius);
    }

    private void applyAura(
            Wolf wolf,
            WonderfulWolfIndividual individual,
            Trait trait,
            PotionEffectType type,
            double radius) {
        TraitStrength strength =
                ExpressedTraitLookup.strength(individual, trait)
                        .orElse(null);
        if (strength == null) {
            return;
        }

        double radiusSquared = radius * radius;
        int amplifier = EffectPolicy.amplifier(trait, strength);
        for (Player player : wolf.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(wolf.getLocation())
                    <= radiusSquared) {
                applyPotion(
                        player,
                        type,
                        EFFECT_TICKS,
                        amplifier,
                        REFRESH_THRESHOLD);
            }
        }
    }
}
