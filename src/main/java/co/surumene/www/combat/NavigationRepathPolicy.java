package co.surumene.www.combat;

/** Excludes expensive replanning when an existing path still reaches the same target. */
public final class NavigationRepathPolicy {
    private static final double TARGET_DISPLACEMENT_SQUARED = 0.25;

    private NavigationRepathPolicy() {}

    public static boolean replan(
            boolean hasPath, boolean targetChanged, double movedSquared) {
        return co.surumene.wcl.api.behavior.RepathDecision.needsRepath(
                hasPath, targetChanged, movedSquared, TARGET_DISPLACEMENT_SQUARED);
    }
}
