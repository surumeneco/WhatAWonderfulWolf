package co.surumene.www.runtime;

import java.util.Collection;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Spatial candidate acquisition; the box result is filtered to the exact original sphere. */
final class AuraNearbyPlayers {
    private AuraNearbyPlayers() {}

    static Collection<Player> find(World world, Location source, double radius) {
        return Objects.requireNonNull(world, "world").getNearbyPlayers(
                Objects.requireNonNull(source, "source"), radius);
    }

    static boolean insideSphere(Location source, Location candidate, double radiusSquared) {
        return source.distanceSquared(candidate) <= radiusSquared;
    }
}
