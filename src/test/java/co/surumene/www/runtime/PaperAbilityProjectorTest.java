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
        RegistrationFixture fixture = new RegistrationFixture(false);
        fixture.project(1.25);
        assertEquals(1, fixture.registerCount.get());
        assertEquals(1.25, fixture.baseValue.get(), 1e-12);
    }

    @Test
    void existingAttributeIsUpdatedWithoutReregistering() {
        RegistrationFixture fixture = new RegistrationFixture(true);
        fixture.project(0.2);
        fixture.project(2.0);
        assertEquals(0, fixture.registerCount.get());
        assertEquals(2.0, fixture.baseValue.get(), 1e-12);
    }

    @Test
    void registersAttributeAgainWhenEntityAttributeIsMissingAfterReload() {
        RegistrationFixture fixture = new RegistrationFixture(false);
        fixture.project(0.4);
        fixture.available.set(false);
        fixture.project(0.9);
        assertEquals(2, fixture.registerCount.get());
        assertEquals(0.9, fixture.baseValue.get(), 1e-12);
    }

    @Test
    void failedAttributeRegistrationDoesNotSilentlyBecomeZeroSpeed() {
        RegistrationFixture fixture = new RegistrationFixture(false);
        fixture.registrationDoesNotInstall = true;
        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> fixture.project(1.0));
        assertTrue(error.getMessage().contains("attack_speed"));
        assertEquals(1, fixture.registerCount.get());
    }

    @Test
    void rejectedAttributeRegistrationReportsTheCause() {
        RegistrationFixture fixture = new RegistrationFixture(false);
        fixture.registrationFailure = new IllegalArgumentException("unsupported");
        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> fixture.project(1.0));
        assertEquals(fixture.registrationFailure, error.getCause());
    }

    private static final class RegistrationFixture {
        final java.util.concurrent.atomic.AtomicBoolean available;
        final java.util.concurrent.atomic.AtomicInteger registerCount =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicReference<Double> baseValue =
                new java.util.concurrent.atomic.AtomicReference<>(1.0);
        boolean registrationDoesNotInstall;
        RuntimeException registrationFailure;

        RegistrationFixture(boolean present) {
            available = new java.util.concurrent.atomic.AtomicBoolean(present);
        }

        void project(double speed) {
            PaperAbilityProjector.projectAttackSpeedSlot(
                    available::get,
                    () -> {
                        registerCount.incrementAndGet();
                        if (registrationFailure != null) throw registrationFailure;
                        if (!registrationDoesNotInstall) available.set(true);
                    },
                    baseValue::set,
                    speed,
                    "fixture-wolf");
        }
    }
}
