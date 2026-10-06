package co.surumene.www.combat;

public final class WonderfulWolfWeaponTiming {
    private WonderfulWolfWeaponTiming() {}

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
