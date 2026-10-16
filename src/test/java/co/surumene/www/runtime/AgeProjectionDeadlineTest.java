package co.surumene.www.runtime;

import co.surumene.www.ability.AgeCurve;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class AgeProjectionDeadlineTest {
    private static final AgeCurve CURVE = new AgeCurve(3, 0.5, 20, 30, 1.0);

    @Test void suppressesReevaluationDuringPeakPlateau() {
        long adult = 1000;
        assertEquals(adult + 20 * 24000,
                AgeProjectionDeadline.next(adult + 10 * 24000, adult, CURVE, List.of()));
    }

    @Test void schedulesUpcomingInjuryBeforeAgingBoundary() {
        long adult = 1000;
        assertEquals(adult + 15 * 24000,
                AgeProjectionDeadline.nextForInjuryDays(
                        adult + 10 * 24000, adult, CURVE, List.of(15.0)));
    }

    @Test void checksEveryBiologicalBucketInGrowthAndAgingRamp() {
        assertEquals(2200L, AgeProjectionDeadline.next(2041, 1000, CURVE, List.of()));
        assertEquals(600200L,
                AgeProjectionDeadline.next(600001, 1000, CURVE, List.of()));
    }

    @Test void elderlyWithNoPendingInjuryNeedsNoAgeRefresh() {
        assertEquals(Long.MAX_VALUE,
                AgeProjectionDeadline.next(1000 + 40 * 24000, 1000, CURVE, List.of()));
    }
}
