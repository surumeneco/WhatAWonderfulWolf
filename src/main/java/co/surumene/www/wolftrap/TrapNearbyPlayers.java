package co.surumene.www.wolftrap;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Restrict activation tests to players inside the encounter's local bounds. */
final class TrapNearbyPlayers {
    private TrapNearbyPlayers() {}

    static Collection<Player> find(World world, Location location, double radius) {
        return world.getNearbyPlayers(location, radius);
    }

    static boolean withinRadius(Location source, Location other, double radiusSquared) {
        return source.distanceSquared(other) <= radiusSquared;
    }
}
