package co.surumene.www.combat;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class WeaponWorkSelectionTest {
    @Test
    void emptyTargetsRequireNoPerTickWeaponWork() {
        assertTrue(WeaponWorkSelection.activeWolves(Map.of()).isEmpty());
    }

    @Test
    void onlyWolvesWithSelectedTargetsAreScheduled() {
        UUID wolfA = UUID.randomUUID();
        UUID wolfB = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Map<UUID, UUID> targets = new HashMap<>();
        targets.put(wolfA, target);
        Set<UUID> selected = WeaponWorkSelection.activeWolves(targets);
        assertEquals(Set.of(wolfA), selected);
        targets.put(wolfB, target);
        assertEquals(Set.of(wolfA), selected, "snapshot cannot change while a tick runs");
    }
}
