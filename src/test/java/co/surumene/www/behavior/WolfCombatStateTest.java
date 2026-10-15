package co.surumene.www.behavior;

import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class WolfCombatStateTest {
    @Test
    void clearsAngerEvenWhenCurrentTargetIsAlreadyNull() {
        AtomicBoolean angry = new AtomicBoolean(true);
        AtomicBoolean targetCleared = new AtomicBoolean(false);
        Wolf wolf = (Wolf) Proxy.newProxyInstance(Wolf.class.getClassLoader(),
                new Class<?>[]{Wolf.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getTarget" -> null;
                        case "setTarget" -> {targetCleared.set(true); yield null;}
                        case "isAngry" -> angry.get();
                        case "setAngry" -> {angry.set((boolean) args[0]); yield null;}
                        default -> BehaviorPaperTestSupport.defaultValue(method.getReturnType());
                    };
                });
        WolfCombatState.clear(wolf);
        assertTrue(targetCleared.get());
        assertFalse(angry.get());
    }
}
