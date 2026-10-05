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
}
