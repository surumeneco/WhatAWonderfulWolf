package co.surumene.www.ability;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AgeCurveMathRegressionTest {
    @Test void matchesKnownWolfMaturityAndElderValues() {
        var curve = new AgeCurve(672.0, 0.5, 3552.0, 7872.0, 1.5);
        assertEquals(0.5, curve.rankReductionAt(0));
        assertEquals(0.25, curve.rankReductionAt(336));
        assertEquals(0.0, curve.rankReductionAt(672));
        assertEquals(0.0, curve.rankReductionAt(3552));
        assertEquals(0.75, curve.rankReductionAt(5712));
        assertEquals(1.5, curve.rankReductionAt(7872));
        assertThrows(IllegalArgumentException.class, () -> curve.rankReductionAt(Double.NaN));
    }
}
