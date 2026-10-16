package co.surumene.www.combat;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Keeps idle wolves out of the per-tick weapon pipeline. */
public final class WeaponWorkSelection {
    private WeaponWorkSelection() {}

    public static Set<UUID> activeWolves(Map<UUID, UUID> selectedTargets) {
        Objects.requireNonNull(selectedTargets, "selectedTargets");
        return selectedTargets.isEmpty()
                ? Set.of()
                : Set.copyOf(selectedTargets.keySet());
    }
}
