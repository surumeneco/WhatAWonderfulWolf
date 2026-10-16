package co.surumene.www.behavior;

import co.surumene.www.domain.Mode;

/** Idle wandering wolves do not require the periodic behavior AI scan. */
public final class BehaviorTickEligibility {
    private BehaviorTickEligibility() {}

    public static boolean periodic(Mode mode) {
        return mode != Mode.WANDER;
    }

    public static boolean evaluate(Mode mode, boolean hasTransientState) {
        return periodic(mode) || hasTransientState;
    }
}
