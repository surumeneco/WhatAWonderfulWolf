package co.surumene.www.wolftrap;

import org.bukkit.event.weather.LightningStrikeEvent;

public final class WolfTrapPolicy {
    public static final long DAY_TICKS = 24_000L;
    public static final long ADDITIONAL_CHECK_TIME = 18_000L;
    public static final double ADDITIONAL_PROBABILITY = 0.01;
    public static final double ADDITIONAL_RADIUS_BLOCKS = 32.0;
    public static final long ADDITIONAL_GRACE_TICKS = 60L;

    private WolfTrapPolicy() {}

    public static boolean isNaturalLightning(
            LightningStrikeEvent.Cause cause) {
        return cause == LightningStrikeEvent.Cause.WEATHER;
    }

    public static boolean reachedAdditionalCheckTime(
            long previousFullTime,
            long currentFullTime) {
        return currentFullTime == previousFullTime + 1L
                && Math.floorMod(currentFullTime, DAY_TICKS)
                        == ADDITIONAL_CHECK_TIME;
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
        return draw < probability;
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

        double distance = StrictMath.sqrt(radialDraw) * radius;
        double angle = angleDraw * StrictMath.PI * 2.0;
        return new Offset(
                StrictMath.cos(angle) * distance,
                StrictMath.sin(angle) * distance);
    }

    public record Offset(double x, double z) {}
}
