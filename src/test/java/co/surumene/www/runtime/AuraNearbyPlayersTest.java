package co.surumene.www.runtime;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AuraNearbyPlayersTest {
    @Test void requestsSpatialCandidatesInsteadOfEnumeratingEveryPlayer() {
        AtomicInteger spatialCalls = new AtomicInteger();
        Player inside = player(new Location(null, 2, 0, 0));
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[]{World.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getPlayers")) {
                        throw new AssertionError("Global player enumeration is not allowed");
                    }
                    if (method.getName().equals("getNearbyPlayers")) {
                        spatialCalls.incrementAndGet();
                        assertEquals(4.0, (Double) args[1], 0.0);
                        return List.of(inside);
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                });
        Collection<Player> candidates = AuraNearbyPlayers.find(world, new Location(null, 0, 0, 0), 4);
        assertEquals(List.of(inside), List.copyOf(candidates));
        assertEquals(1, spatialCalls.get());
    }

    @Test void cubeCandidatesStillRequireOriginalSphericalDistanceCheck() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[]{World.class}, (proxy, method, args) -> null);
        Location source = new Location(world, 0, 0, 0);
        assertTrue(AuraNearbyPlayers.insideSphere(source, new Location(world, 3, 0, 0), 16));
        assertFalse(AuraNearbyPlayers.insideSphere(source, new Location(world, 3, 3, 0), 16));
    }

    private static Player player(Location location) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[]{Player.class}, (proxy, method, args) ->
                    method.getName().equals("getLocation") ? location : null);
    }
}
