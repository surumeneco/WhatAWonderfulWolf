package co.surumene.www.behavior;

import java.util.Objects;

public final class RetreatPolicy {
    private RetreatPolicy() {}

    public static double threshold(double patience) {
        if (!Double.isFinite(patience) || patience < 0.0) {
            throw new IllegalArgumentException(
                    "patience must be finite and >= 0");
        }
        return Math.max(0.0, Math.min(1.0, 1.0 - patience));
    }

    public static RetreatState evaluate(
            RetreatState state,
            double healthFraction,
            double patience,
            long currentTick,
            long minimumRetreatTicks) {
        Objects.requireNonNull(state, "state");
        if (!Double.isFinite(healthFraction)
                || healthFraction < 0.0
                || healthFraction > 1.0) {
            throw new IllegalArgumentException(
                    "healthFraction must be within [0,1]");
        }
        if (currentTick < 0L || minimumRetreatTicks < 0L) {
            throw new IllegalArgumentException(
                    "ticks must be >= 0");
        }

        double threshold = threshold(patience);
        boolean condition = healthFraction <= threshold;
        if (!state.active()) {
            return condition
                    ? new RetreatState(true, currentTick)
                    : state;
        }

        long elapsed = Math.max(
                0L,
                currentTick - state.startedTick());
        if (elapsed < minimumRetreatTicks || condition) {
            return state;
        }
        return RetreatState.inactive();
    }
}
