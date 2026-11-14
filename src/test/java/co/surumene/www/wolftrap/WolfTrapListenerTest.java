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
    void preparationCancelsDamageAndFeedbackIsSpecificToEntityAttack() {
        EntityDamageEvent environment = mock(EntityDamageEvent.class);
        when(environment.getEntity()).thenReturn(waiting);
        when(runtime.protectDamage(waiting, false)).thenReturn(true);
        listener.onProtect(environment);
        verify(environment).setCancelled(true);
        verify(runtime).protectDamage(waiting, false);

        EntityDamageByEntityEvent attack = mock(EntityDamageByEntityEvent.class);
        when(attack.getEntity()).thenReturn(waiting);
        when(runtime.protectDamage(waiting, true)).thenReturn(true);
        listener.onProtect(attack);
        verify(attack).setCancelled(true);
        verify(runtime).protectDamage(waiting, true);
    }
}
