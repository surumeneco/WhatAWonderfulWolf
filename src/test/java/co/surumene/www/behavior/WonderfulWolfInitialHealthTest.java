package co.surumene.www.behavior;

import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfCreationResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfInitialHealthTest {
    @Test
    void newWwwStartsAtProjectedMaximumButLaterSaveDoesNotHeal() {
        World world = BehaviorPaperTestSupport.world(UUID.randomUUID());
        var fixture = BehaviorPaperTestSupport.wolf(
                UUID.randomUUID(), world, new BehaviorPaperTestSupport.MemoryPdc(),
                new Location(world, 0, 64, 0), false);
        AtomicReference<Double> health = new AtomicReference<>(8.0);
        AttributeInstance maxHealth = (AttributeInstance) Proxy.newProxyInstance(
                AttributeInstance.class.getClassLoader(),
                new Class<?>[] {AttributeInstance.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getValue", "getBaseValue" -> 50.0;
                    default -> BehaviorPaperTestSupport.defaultValue(method.getReturnType());
                });
        Wolf wolf = (Wolf) Proxy.newProxyInstance(
                Wolf.class.getClassLoader(), new Class<?>[] {Wolf.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> args[0] == Attribute.MAX_HEALTH ? maxHealth : null;
                    case "getHealth" -> health.get();
                    case "setHealth" -> { health.set((Double) args[0]); yield null; }
                    default -> method.invoke(fixture.wolf(), args);
                });

        var loaded = BehaviorPaperTestSupport.loaded();
        AtomicInteger projections = new AtomicInteger();
        var individual = BehaviorTestIndividuals.individual(0, 10);
        new PaperWonderfulWolfFactory(
                (origin, owner, adultTime, seed) ->
                        new WonderfulWolfCreationResult.Success(individual),
                loaded, () -> 0L, entity -> projections.incrementAndGet());

        loaded.saveAndRegister(wolf, individual);
        assertEquals(50.0, health.get());
        assertEquals(1, projections.get());

        health.set(12.0);
        loaded.saveAndRegister(wolf, individual);
        assertEquals(12.0, health.get());
        assertEquals(1, projections.get());
    }
}
