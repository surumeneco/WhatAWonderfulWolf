package co.surumene.www.runtime;
import co.surumene.www.ability.AgeCurve;
import co.surumene.wcl.api.time.AgeDeadlineMath;
import java.util.List;
/** Consumer contract and guarded overflow behavior remain WWW-owned. */
final class AgeProjectionDeadline {
    private AgeProjectionDeadline() {}
    static long next(long now,long adultStart,AgeCurve curve,List<Double> injuryOnsets) {
        return nextForInjuryDays(now,adultStart,curve,injuryOnsets);
    }
    static long nextForInjuryDays(long now,long adultStart,AgeCurve curve,List<Double> injuryOnsets) {
        return AgeDeadlineMath.next(now,adultStart,curve.growthEndGameDay(),
                curve.agingStartGameDay(),curve.elderGameDay(),injuryOnsets,AgeDeadlineMath.BoundaryMode.GUARDED);
    }
}
