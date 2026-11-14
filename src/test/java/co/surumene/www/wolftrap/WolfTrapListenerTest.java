package co.surumene.www.wolftrap;

import org.bukkit.entity.Evoker;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class WolfTrapListenerTest {
    private final WolfTrapRuntime runtime = mock(WolfTrapRuntime.class);
    private final WolfTrapListener listener = new WolfTrapListener(runtime);
    private final Evoker waiting = mock(Evoker.class);

    @Test
    void onlyPlayerOrPlayerProjectileCanTriggerTrap() {
        EntityDamageByEntityEvent mobAttack = mock(EntityDamageByEntityEvent.class);
        when(mobAttack.getEntity()).thenReturn(waiting);
        when(mobAttack.getDamager()).thenReturn(mock(Zombie.class));
        listener.onAttack(mobAttack);
        verify(runtime, never()).activateByAttack(waiting);

        EntityDamageByEntityEvent playerAttack = mock(EntityDamageByEntityEvent.class);
        when(playerAttack.getEntity()).thenReturn(waiting);
        when(playerAttack.getDamager()).thenReturn(mock(Player.class));
        when(runtime.activateByAttack(waiting)).thenReturn(true);
        listener.onAttack(playerAttack);
        verify(runtime).activateByAttack(waiting);
        verify(playerAttack).setCancelled(true);

        EntityDamageByEntityEvent projectileAttack = mock(EntityDamageByEntityEvent.class);
        Projectile arrow = mock(Projectile.class);
        when(arrow.getShooter()).thenReturn(mock(Player.class));
        when(projectileAttack.getEntity()).thenReturn(waiting);
        when(projectileAttack.getDamager()).thenReturn(arrow);
        listener.onAttack(projectileAttack);
        verify(runtime, times(2)).activateByAttack(waiting);
        verify(projectileAttack).setCancelled(true);
    }

    @Test
    void preparationCancelsDamageBeforeRenderingFeedback() {
        EntityDamageEvent environment = mock(EntityDamageEvent.class);
        when(environment.getEntity()).thenReturn(waiting);
        when(runtime.protectDamage(waiting)).thenReturn(true);
        listener.onProtect(environment);
        org.mockito.InOrder environmentOrder = inOrder(runtime, environment);
        environmentOrder.verify(runtime).protectDamage(waiting);
        environmentOrder.verify(environment).setCancelled(true);
        verify(runtime, never()).playProtectedHitEffect(waiting);

        clearInvocations(runtime);
        EntityDamageByEntityEvent attack = mock(EntityDamageByEntityEvent.class);
        when(attack.getEntity()).thenReturn(waiting);
        listener.onProtect(attack);
        org.mockito.InOrder attackOrder = inOrder(runtime, attack);
        attackOrder.verify(runtime).protectDamage(waiting);
        attackOrder.verify(attack).setCancelled(true);
        attackOrder.verify(runtime).playProtectedHitEffect(waiting);
    }

    @Test
    void feedbackFailureDoesNotUndoCancellation() {
        EntityDamageByEntityEvent attack = mock(EntityDamageByEntityEvent.class);
        when(attack.getEntity()).thenReturn(waiting);
        when(runtime.protectDamage(waiting)).thenReturn(true);
        doThrow(new IllegalStateException("particle backend unavailable"))
                .when(runtime).playProtectedHitEffect(waiting);

        try {
            listener.onProtect(attack);
        } catch (IllegalStateException expected) {
            // The event must have been cancelled even if feedback fails.
        }
        verify(attack).setCancelled(true);
    }

}
