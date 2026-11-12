package co.surumene.www.runtime;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.individual.WorldPosition;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

final class WonderfulWolfWatchmanCenterTest {
    @Test
    void detectionAndGlowAlwaysUseWolfsCurrentLocationInAllModes() {
        for (Mode mode : Mode.values()) {
            assertWatchmanCenteredOnWolf(mode, true);
        }
    }

    @Test
    void detectionDoesNotDependOnCommanderBeingOnline() {
        assertWatchmanCenteredOnWolf(Mode.FOLLOW, false);
        assertWatchmanCenteredOnWolf(Mode.GUARD, false);
        assertWatchmanCenteredOnWolf(Mode.WAIT, false);
    }

    private static void assertWatchmanCenteredOnWolf(Mode mode, boolean commanderOnline) {
        UUID wolfId = UUID.randomUUID();
        UUID commanderId = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        World world = mock(World.class);
        Server server = mock(Server.class);
        Wolf wolf = mock(Wolf.class);
        Player commander = mock(Player.class);
        Zombie nearbyEnemy = mock(Zombie.class);
        WonderfulWolfIndividual individual = mock(WonderfulWolfIndividual.class);
        PhenotypeSnapshot phenotype = mock(PhenotypeSnapshot.class);
        WonderfulWolfLoadedIndividuals loaded = mock(WonderfulWolfLoadedIndividuals.class);
        WonderfulWolfBehaviorRuntime behavior = mock(WonderfulWolfBehaviorRuntime.class);
        WwwConfig.Runtime settings = mock(WwwConfig.Runtime.class);

        Location wolfLocation = new Location(world, 0.5, 64.0, 0.5);
        when(wolf.getUniqueId()).thenReturn(wolfId);
        when(wolf.getWorld()).thenReturn(world);
        when(wolf.getLocation()).thenReturn(wolfLocation);
        when(world.getUID()).thenReturn(worldId);
        when(world.getNearbyEntities(any(Location.class), eq(15.0), eq(15.0), eq(15.0)))
                .thenReturn(List.of((Entity) nearbyEnemy));

        when(nearbyEnemy.getUniqueId()).thenReturn(UUID.randomUUID());
        when(nearbyEnemy.getLocation()).thenReturn(new Location(world, 3.5, 64, 0.5));
        when(nearbyEnemy.isValid()).thenReturn(true);
        when(nearbyEnemy.getType()).thenReturn(EntityType.ZOMBIE);

        when(individual.phenotypeSnapshot()).thenReturn(phenotype);
        when(phenotype.expressedTraits()).thenReturn(
                List.of(new ExpressedTrait(Trait.WATCHMAN, TraitStrength.WEAK)));
        when(individual.mode()).thenReturn(mode);
        when(individual.actionDistance()).thenReturn(ActionDistance.NORMAL);
        when(individual.commanderId()).thenReturn(
                mode == Mode.WANDER ? Optional.empty() : Optional.of(commanderId));
        if (mode == Mode.WAIT) {
            when(individual.waitLocation()).thenReturn(
                    Optional.of(new WorldPosition(worldId, 500.0, 64.0, 0.0)));
        }

        when(server.getWorld(worldId)).thenReturn(world);
        when(server.getPlayer(commanderId)).thenReturn(commander);
        when(commander.isOnline()).thenReturn(commanderOnline);
        when(commander.getLocation()).thenReturn(new Location(world, 500, 64, 0));
        when(loaded.commanderWolfIds(commanderId)).thenReturn(Set.of());
        when(settings.actionDistance()).thenReturn(new WwwConfig.ActionDistance(5, 10, 20, 30));
        when(settings.traits()).thenReturn(new WwwConfig.Traits(7.5, 1.5, 10, 80));

        new WonderfulWolfWatchmanRuntime(loaded, behavior, server)
                .tick(wolf, individual, settings, 100);

        verify(world).getNearbyEntities(eq(wolfLocation), eq(15.0), eq(15.0), eq(15.0));
        verify(nearbyEnemy).addPotionEffect(any(PotionEffect.class));
        verify(server, never()).getPlayer(any(UUID.class));
        verify(server, never()).getWorld(any(UUID.class));
    }
}
