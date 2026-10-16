package co.surumene.www.behavior;

import co.surumene.www.domain.Mode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PassiveRelationshipEligibilityTest {
  @Test void schedulesOnlyCommanderFollowAndGuard() {
    UUID owner = UUID.randomUUID();
    assertTrue(PassiveRelationshipEligibility.eligible(Mode.FOLLOW, owner));
    assertTrue(PassiveRelationshipEligibility.eligible(Mode.GUARD, owner));
    assertFalse(PassiveRelationshipEligibility.eligible(Mode.WAIT, owner));
    assertFalse(PassiveRelationshipEligibility.eligible(Mode.WANDER, owner));
    assertFalse(PassiveRelationshipEligibility.eligible(Mode.FOLLOW, null));
  }
}
