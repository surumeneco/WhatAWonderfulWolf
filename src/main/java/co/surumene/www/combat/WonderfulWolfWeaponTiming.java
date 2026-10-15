package co.surumene.www.combat;

public final class WonderfulWolfWeaponTiming {
    private WonderfulWolfWeaponTiming() {}

    public static final double MIN_ATTACK_RATE = 0.01;

    /** Actual attack scheduler rate: a zero effective attribute must not permanently block attacks. */
    public static double attackRate(double attributeValue) {
        if (!Double.isFinite(attributeValue)) {
            throw new IllegalArgumentException("attack speed must be finite");
        }
        return Math.max(MIN_ATTACK_RATE, attributeValue);
    }

    public static long cooldownTicks(double attacksPerSecond) {
        if (!Double.isFinite(attacksPerSecond) || attacksPerSecond < 0.0) {
            throw new IllegalArgumentException(
                    "attacksPerSecond must be finite and >= 0");
        }
        if (attacksPerSecond == 0.0) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) Math.ceil(20.0 / attacksPerSecond));
    }
}
