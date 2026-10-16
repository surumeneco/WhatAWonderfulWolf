package co.surumene.www.runtime;

import co.surumene.www.ability.AgeCurve;
import java.util.List;

/** Earliest biological time at which a projected ability can change. */
final class AgeProjectionDeadline {
    private static final double TICKS_PER_DAY = 24_000.0;
    private static final long QUANTUM = 200;

    private AgeProjectionDeadline() {}

    static long next(long now, long adultStart, AgeCurve curve, List<Double> injuryOnsets) {
        return nextForInjuryDays(now, adultStart, curve, injuryOnsets);
    }

    static long nextForInjuryDays(long now, long adultStart, AgeCurve curve, List<Double> injuryOnsets) {
        double age = Math.max(0, now - adultStart) / TICKS_PER_DAY;
        long due;
        if (age < curve.growthEndGameDay() || (age >= curve.agingStartGameDay()
                && age < curve.elderGameDay())) {
            due = (now / QUANTUM + 1L) * QUANTUM;
        } else if (age < curve.agingStartGameDay()) {
            due = deadline(adultStart, curve.agingStartGameDay());
        } else {
            due = Long.MAX_VALUE;
        }
        for (Double onset : injuryOnsets) {
            if (onset != null && Double.isFinite(onset) && onset > age) {
                due = Math.min(due, deadline(adultStart, onset));
            }
        }
        return due;
    }

    private static long deadline(long start, double gameDay) {
        if (!Double.isFinite(gameDay) || gameDay < 0
                || gameDay > (Long.MAX_VALUE - Math.max(0L, start)) / TICKS_PER_DAY) {
            return Long.MAX_VALUE;
        }
        return Math.max(start, start + (long) Math.ceil(gameDay * TICKS_PER_DAY));
    }
}
