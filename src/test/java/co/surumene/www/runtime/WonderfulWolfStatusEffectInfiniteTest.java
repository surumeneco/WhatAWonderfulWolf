package co.surumene.www.runtime;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ensure WWW's Paper adapter respects WCL's infinite-duration semantics. */
class WonderfulWolfStatusEffectInfiniteTest {
    @Test
    void preservesExistingInfiniteSameLevelEffect() {
        LivingEntity wolf = mock(LivingEntity.class);
        PotionEffect existing = mock(PotionEffect.class);
        when(existing.getAmplifier()).thenReturn(0);
        when(existing.getDuration()).thenReturn(-1);
        when(wolf.getPotionEffect(PotionEffectType.FIRE_RESISTANCE)).thenReturn(existing);

        WonderfulWolfStatusEffectRuntime.applyPotion(
                wolf, PotionEffectType.FIRE_RESISTANCE, 80, 0, 40);

        verify(wolf, never()).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void preservesStrongerInfiniteEffect() {
        LivingEntity wolf = mock(LivingEntity.class);
        PotionEffect existing = mock(PotionEffect.class);
        when(existing.getAmplifier()).thenReturn(1);
        when(existing.getDuration()).thenReturn(-1);
        when(wolf.getPotionEffect(PotionEffectType.FIRE_RESISTANCE)).thenReturn(existing);

        WonderfulWolfStatusEffectRuntime.applyPotion(
                wolf, PotionEffectType.FIRE_RESISTANCE, 80, 0, 40);

        verify(wolf, never()).addPotionEffect(any(PotionEffect.class));
    }
}
