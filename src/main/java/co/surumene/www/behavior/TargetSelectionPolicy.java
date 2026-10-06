package co.surumene.www.behavior;

import co.surumene.www.domain.Mode;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TargetSelectionPolicy {
    private TargetSelectionPolicy() {}

    public static Optional<TargetCandidate> select(
            Mode mode,
            boolean retreating,
            List<TargetCandidate> candidates) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(candidates, "candidates");
        if (retreating) return Optional.empty();

        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> allowed(mode, candidate))
                .min(Comparator
                        .<TargetCandidate>comparingInt(
                                candidate -> priority(candidate.source()))
                        .thenComparingDouble(
                                candidate -> distance(mode, candidate))
                        .thenComparing(
                                candidate -> candidate.targetId().toString()));
    }

    private static boolean allowed(
            Mode mode,
            TargetCandidate candidate) {
        if (mode == Mode.WANDER) {
            return candidate.source() == TargetSource.TRAP_RIDER
                    || candidate.source() == TargetSource.SELF_ATTACKER;
        }
        if (!candidate.withinActionDistance()) return false;
        if (mode == Mode.FOLLOW
                && candidate.source() == TargetSource.ACTIVE_SEARCH) {
            return false;
        }
        return true;
    }

    private static int priority(TargetSource source) {
        return switch (source) {
            case MANUAL, TRAP_RIDER -> 0;
            case SELF_ATTACKER, COMMAND_COMBAT -> 1;
            case PVP_INTERVENTION -> 2;
            case ACTIVE_SEARCH -> 3;
        };
    }

    private static double distance(
            Mode mode,
            TargetCandidate candidate) {
        return mode == Mode.FOLLOW || mode == Mode.WANDER
                ? candidate.wolfDistanceSquared()
                : candidate.referenceDistanceSquared();
    }
}
