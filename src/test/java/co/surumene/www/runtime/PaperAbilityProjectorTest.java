package co.surumene.www.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PaperAbilityProjectorTest {
    @Test
    void movementSpeedUsesWwcBlocksPerSecondCalibration() {
        assertEquals(
                1.0,
                PaperAbilityProjector.movementAttributeForBlocksPerSecond(42.157),
                1.0e-12);
        assertEquals(
                0.1,
                PaperAbilityProjector.movementAttributeForBlocksPerSecond(4.2157),
                1.0e-12);
    }

    @Test
    void jumpHeightUsesWwcVelocityCalibration() {
        assertEquals(
                0.42,
                PaperAbilityProjector.jumpStrengthForHeight(1.25),
                1.0e-12);
        assertEquals(
                0.84,
                PaperAbilityProjector.jumpStrengthForHeight(5.0),
                1.0e-12);
    }

    @Test
    void registersMissingAttackSpeedAndAppliesBaseValue() {
        Fixture fixture = new Fixture(false);
        new PaperAbilityProjector(java.util.logging.Logger.getAnonymousLogger())
                .projectAttackSpeed(fixture.wolf, 1.25);
        assertEquals(1, fixture.registerCount.get());
        assertEquals(1.25, fixture.baseValue.get(), 1e-12);
    }

    @Test
    void existingAttributeIsUpdatedWithoutReregistering() {
        Fixture fixture = new Fixture(true);
        PaperAbilityProjector projector = new PaperAbilityProjector(
                java.util.logging.Logger.getAnonymousLogger());
        projector.projectAttackSpeed(fixture.wolf, 0.2);
        projector.projectAttackSpeed(fixture.wolf, 2.0);
        assertEquals(0, fixture.registerCount.get());
        assertEquals(2.0, fixture.baseValue.get(), 1e-12);
    }

    @Test
    void registersAttributeAgainWhenEntityAttributeIsMissingAfterReload() {
        Fixture fixture = new Fixture(false);
        PaperAbilityProjector projector = new PaperAbilityProjector(
                java.util.logging.Logger.getAnonymousLogger());
        projector.projectAttackSpeed(fixture.wolf, 0.4);
        fixture.available.set(false);
        projector.projectAttackSpeed(fixture.wolf, 0.9);
        assertEquals(2, fixture.registerCount.get());
        assertEquals(0.9, fixture.baseValue.get(), 1e-12);
    }

    @Test
    void failedAttributeRegistrationDoesNotSilentlyBecomeZeroSpeed() {
        Fixture fixture = new Fixture(false);
        fixture.registrationDoesNotInstall = true;
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> new PaperAbilityProjector(java.util.logging.Logger.getAnonymousLogger())
                        .projectAttackSpeed(fixture.wolf, 1.0));
        assertTrue(error.getMessage().contains("attack_speed"));
        assertEquals(1, fixture.registerCount.get());
    }

    private static final class Fixture {
        final java.util.concurrent.atomic.AtomicBoolean available;
        final java.util.concurrent.atomic.AtomicInteger registerCount =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicReference<Double> baseValue =
                new java.util.concurrent.atomic.AtomicReference<>(1.0);
        boolean registrationDoesNotInstall;
        final org.bukkit.entity.Wolf wolf;

        Fixture(boolean present) {
            available = new java.util.concurrent.atomic.AtomicBoolean(present);
            org.bukkit.attribute.AttributeInstance attribute =
                    (org.bukkit.attribute.AttributeInstance) java.lang.reflect.Proxy.newProxyInstance(
                            org.bukkit.attribute.AttributeInstance.class.getClassLoader(),
                            new Class<?>[]{org.bukkit.attribute.AttributeInstance.class},
                            (proxy, method, args) -> switch (method.getName()) {
                                case "getBaseValue", "getValue" -> baseValue.get();
                                case "setBaseValue" -> { baseValue.set((Double) args[0]); yield null; }
                                default -> null;
                            });
            wolf = (org.bukkit.entity.Wolf) java.lang.reflect.Proxy.newProxyInstance(
                    org.bukkit.entity.Wolf.class.getClassLoader(),
                    new Class<?>[]{org.bukkit.entity.Wolf.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getAttribute" -> available.get()
                                && args[0] == org.bukkit.attribute.Attribute.ATTACK_SPEED
                                ? attribute : null;
                        case "registerAttribute" -> {
                            registerCount.incrementAndGet();
                            if (!registrationDoesNotInstall) available.set(true);
                            yield null;
                        }
                        case "getUniqueId" -> new java.util.UUID(1L, 2L);
                        default -> null;
                    });
        }
    }
}
