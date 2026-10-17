package co.surumene.www.runtime;
public final class AbilityWakeupPolicy {
    private AbilityWakeupPolicy() {}
    public static long delay(long t, long b, boolean dirty, long juvenile, long audit, long due) {
        if (dirty) return 1;
        long next = Math.min(Math.max(1, juvenile - t), Math.max(1, audit - t));
        return due == Long.MAX_VALUE ? next : Math.min(next, Math.max(1, due - b));
    }
}
