package co.surumene.www.runtime;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.Collection;
import java.util.Objects;

/** Preserves the consumer-facing query helper while delegating Paper details to WCL. */
final class AuraNearbyPlayers {
    private AuraNearbyPlayers() {}

    static Collection<Player> find(World world, Location source, double radius) {
        return co.surumene.wcl.plugin.spatial.NearbyPlayerQuery.find(world,
                Objects.requireNonNull(source, "source"), radius);
    }

    static boolean insideSphere(Location source, Location player, double radiusSquared) {
        return co.surumene.wcl.plugin.spatial.NearbyPlayerQuery.insideSphere(
                source, player, radiusSquared);
    }
}
