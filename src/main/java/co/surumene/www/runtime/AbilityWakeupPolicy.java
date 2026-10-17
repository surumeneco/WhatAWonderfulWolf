package co.surumene.www.runtime;

/** Selects the next main-thread wakeup, without allocating one Bukkit task per Wolf. */
public final class AbilityWakeupPolicy {
    private static final long MAX_UNNOTIFIED_CLOCK_GAP = 200L;

    private AbilityWakeupPolicy() {}

    public static long delay(long tick, boolean dirty, boolean loaded, boolean juvenile,
                             long juvenileTick, long compatibilityTick,
                             long nextBiologicalDue, long biologicalNow) {
        if (dirty) return 1L;
        if (!loaded) return Long.MAX_VALUE;
        long nextServerDeadline = compatibilityTick;
        if (juvenile) nextServerDeadline = Math.min(nextServerDeadline, juvenileTick);
        long serverWait = Math.min(MAX_UNNOTIFIED_CLOCK_GAP,
                Math.max(1L, nextServerDeadline - tick));
        if (nextBiologicalDue != Long.MAX_VALUE) {
            long biologicalWait = Math.max(1L, nextBiologicalDue - biologicalNow);
            serverWait = Math.min(serverWait, Math.min(MAX_UNNOTIFIED_CLOCK_GAP, biologicalWait));
        }
        return serverWait;
    }
}
