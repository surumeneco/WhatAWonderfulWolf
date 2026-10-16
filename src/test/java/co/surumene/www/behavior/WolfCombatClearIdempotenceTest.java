package co.surumene.www.behavior;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WolfCombatClearIdempotenceTest {
    @Test void idleWolfCausesNoSetterCalls() {
        AtomicInteger targetUpdates = new AtomicInteger();
        AtomicInteger angerUpdates = new AtomicInteger();
        Wolf wolf = wolf(false, false, targetUpdates, angerUpdates);
        WolfCombatState.clear(wolf);
        WolfCombatState.clear(wolf);
        assertEquals(0, targetUpdates.get());
        assertEquals(0, angerUpdates.get());
    }

    @Test void angryWolfIsClearedOnlyOnce() {
        AtomicInteger targetUpdates = new AtomicInteger();
        AtomicInteger angerUpdates = new AtomicInteger();
        Wolf wolf = wolf(true, true, targetUpdates, angerUpdates);
        WolfCombatState.clear(wolf);
        WolfCombatState.clear(wolf);
        assertEquals(1, targetUpdates.get());
        assertEquals(1, angerUpdates.get());
    }

    private static Wolf wolf(boolean targeted, boolean angry,
                             AtomicInteger targets, AtomicInteger anger) {
        AtomicBoolean targetPresent = new AtomicBoolean(targeted);
        AtomicBoolean isAngry = new AtomicBoolean(angry);
        return (Wolf) Proxy.newProxyInstance(Wolf.class.getClassLoader(),
                new Class<?>[]{Wolf.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTarget" -> targetPresent.get() ? proxy : null;
                    case "setTarget" -> { targets.incrementAndGet(); targetPresent.set(false); yield null; }
                    case "isAngry" -> isAngry.get();
                    case "setAngry" -> { anger.incrementAndGet(); isAngry.set((boolean) args[0]); yield null; }
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
    }
}
