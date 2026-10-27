package co.surumene.www.wolftrap;

import org.bukkit.event.weather.LightningStrikeEvent;

public final class WolfTrapPolicy {
    public static final long DAY_TICKS = 24_000L;
    public static final long ADDITIONAL_CHECK_TIME = 18_000L;
    public static final double ADDITIONAL_RADIUS_BLOCKS = 32.0;
    public static final long ADDITIONAL_GRACE_MILLIS = 3_000L;

    private WolfTrapPolicy() {}

    public static boolean isNaturalLightning(
            LightningStrikeEvent.Cause cause) {
        return cause == LightningStrikeEvent.Cause.WEATHER;
    }

    public static boolean reachedAdditionalCheckTime(
            long previousFullTime,
            long currentFullTime) {
        return co.surumene.wcl.api.runtime.EncounterMath.naturalArrival(
                previousFullTime, currentFullTime, DAY_TICKS, ADDITIONAL_CHECK_TIME);
    }

    /** Only a live, mutually paired Evoker may steer its marked trap Wolf. */
    public static boolean riderOwnsNavigation(
            boolean markedMount, boolean pairedPassenger, boolean liveRider) {
        return markedMount && pairedPassenger && liveRider;
    }

    public static boolean roll(
            double draw,
            double probability) {
        if (!Double.isFinite(draw)
                || draw < 0.0
                || draw >= 1.0) {
            throw new IllegalArgumentException(
                    "draw must be finite and in [0, 1)");
        }
        if (!Double.isFinite(probability)
                || probability < 0.0
                || probability > 1.0) {
            throw new IllegalArgumentException(
                    "probability must be finite and in [0, 1]");
        }
        return co.surumene.wcl.api.runtime.EncounterMath.roll(draw, probability);
    }

    public static boolean inGrace(
            long currentFullTime,
            long graceUntilFullTime) {
        return currentFullTime < graceUntilFullTime;
    }

    public static Offset offset(
            double radialDraw,
            double angleDraw,
            double radius) {
        if (!Double.isFinite(radialDraw)
                || radialDraw < 0.0
                || radialDraw > 1.0) {
            throw new IllegalArgumentException(
                    "radialDraw must be finite and in [0, 1]");
        }
        if (!Double.isFinite(angleDraw)
                || angleDraw < 0.0
                || angleDraw >= 1.0) {
            throw new IllegalArgumentException(
                    "angleDraw must be finite and in [0, 1)");
        }
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException(
                    "radius must be finite and >= 0");
        }

        var offset = co.surumene.wcl.api.runtime.EncounterMath.diskOffset(
                radialDraw, angleDraw, radius);
        return new Offset(offset.x(), offset.z());
    }

    public record Offset(double x, double z) {}
}
