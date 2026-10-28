package co.surumene.www.runtime;

/** An active periodic trait or an in-flight HolyPulse is the only reason to schedule trait ticks. */
public final class TraitTaskActivityPolicy {
    private TraitTaskActivityPolicy() {}

    public static boolean shouldRun(int activeSources, boolean pulsesActive) {
        return co.surumene.wcl.api.runtime.TraitActivityGate.shouldRun(activeSources, pulsesActive);
    }

    public static boolean shouldStop(int activeSources, boolean pulsesActive) {
        return co.surumene.wcl.api.runtime.TraitActivityGate.shouldStop(activeSources, pulsesActive);
    }
}
