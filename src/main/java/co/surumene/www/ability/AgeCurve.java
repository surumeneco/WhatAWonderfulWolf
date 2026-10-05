package co.surumene.www.ability;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.PhenotypeSnapshot;

import java.util.Map;
import java.util.Objects;

public record AgeCurve(
        double growthEndGameDay,
        double juvenileSuppressionRank,
        double agingStartGameDay,
        double elderGameDay,
        double maximumAgingReductionRank) {

    public AgeCurve {
        if (!finiteNonNegative(growthEndGameDay)
                || !finiteNonNegative(juvenileSuppressionRank)
                || !finiteNonNegative(agingStartGameDay)
                || !finiteNonNegative(elderGameDay)
                || !finiteNonNegative(maximumAgingReductionRank)
                || agingStartGameDay < growthEndGameDay
                || elderGameDay < agingStartGameDay) {
            throw new IllegalArgumentException("invalid age curve");
        }
    }

    public static AgeCurve from(
            PhenotypeSnapshot snapshot,
            WwwConfig.Age config) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(config, "config");
        Map<DevelopmentFactor, Double> factors = snapshot.developmentFactors();

        double growth =
                config.baseGrowthGameDays()
                        * (1.25 - 0.50 * factors.get(DevelopmentFactor.MATURITY));
        double juvenile =
                0.25 + 0.50 * factors.get(DevelopmentFactor.JUVENILE_SUPPRESSION);
        double peak =
                config.basePeakDurationGameDays()
                        * (0.80 + 0.40 * factors.get(DevelopmentFactor.AGING_START));
        double agingStart = growth + peak;
        double agingDuration =
                config.baseAgingDurationGameDays()
                        * (1.25 - 0.50 * factors.get(DevelopmentFactor.AGING_SPEED));
        double elder = agingStart + agingDuration;
        double maximumReduction =
                2.00 - factors.get(DevelopmentFactor.AGING_RESISTANCE);

        return new AgeCurve(
                growth,
                juvenile,
                agingStart,
                elder,
                maximumReduction);
    }

    public double rankReductionAt(double ageGameDays) {
        if (!Double.isFinite(ageGameDays)) {
            throw new IllegalArgumentException("ageGameDays must be finite");
        }
        double age = Math.max(0.0, ageGameDays);
        if (age < growthEndGameDay) {
            if (growthEndGameDay == 0.0) return 0.0;
            return juvenileSuppressionRank
                    * (1.0 - smoothstep(age / growthEndGameDay));
        }
        if (age < agingStartGameDay) {
            return 0.0;
        }
        if (age < elderGameDay) {
            double duration = elderGameDay - agingStartGameDay;
            if (duration == 0.0) return maximumAgingReductionRank;
            return maximumAgingReductionRank
                    * smoothstep((age - agingStartGameDay) / duration);
        }
        return maximumAgingReductionRank;
    }

    public static double smoothstep(double x) {
        double clamped = Math.max(0.0, Math.min(1.0, x));
        return 3.0 * clamped * clamped
                - 2.0 * clamped * clamped * clamped;
    }

    private static boolean finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }
}
