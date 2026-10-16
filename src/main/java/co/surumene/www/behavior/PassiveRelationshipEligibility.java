package co.surumene.www.behavior;

import co.surumene.www.domain.Mode;
import java.util.UUID;

/** Determines whose passive affection needs scheduled processing. */
public final class PassiveRelationshipEligibility {
    private PassiveRelationshipEligibility() {}

    public static boolean eligible(Mode mode, UUID commander) {
        return commander != null && (mode == Mode.FOLLOW || mode == Mode.GUARD);
    }
}
