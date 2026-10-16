package co.surumene.www.wolftrap;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TrapNearbyPlayersTest {
    @Test void queriesOnlyNearbyPlayersWithoutGlobalEnumerations() {
        AtomicInteger queried = new AtomicInteger();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[]{World.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getPlayers")) {
                        throw new AssertionError("Avoid global player lookup");
                    }
                    if (method.getName().equals("getNearbyPlayers")) {
                        queried.incrementAndGet();
                        assertEquals(6.0, (Double) args[1], 0.0);
                        return List.<Player>of();
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                });
        assertTrue(TrapNearbyPlayers.find(world, new Location(world, 0, 0, 0), 6).isEmpty());
        assertEquals(1, queried.get());
    }

    @Test void retainsSphericalActivationThreshold() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[]{World.class}, (proxy, method, args) -> null);
        var origin = new Location(world, 0, 0, 0);
        assertTrue(TrapNearbyPlayers.withinRadius(origin, new Location(world, 3, 0, 0), 25));
        assertFalse(TrapNearbyPlayers.withinRadius(origin, new Location(world, 4, 4, 0), 25));
    }
}
