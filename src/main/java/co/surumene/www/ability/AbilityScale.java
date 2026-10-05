package co.surumene.www.ability;

import co.surumene.www.domain.Ability;

import java.util.Objects;

public final class AbilityScale {
    private AbilityScale() {}

    public static double toCanonical(Ability ability, double normalized) {
        Objects.requireNonNull(ability, "ability");
        if (!Double.isFinite(normalized) || normalized < 0.0) {
            throw new IllegalArgumentException("normalized must be finite and >= 0");
        }

        double value = switch (ability) {
            case HEALTH -> 20.0 + 40.0 * normalized;
            case SIZE -> 1.5 + 1.5 * normalized;
            case MOVEMENT_SPEED -> 3.0 + 21.0 * normalized;
            case JUMP -> 1.0 + 4.0 * normalized;
            case STEP_HEIGHT -> 0.5 + normalized;
            case ATTACK_DAMAGE -> 1.0 + 9.0 * normalized;
            case ATTACK_SPEED -> 0.2 + 1.8 * normalized;
            case DEFENSE -> 30.0 * normalized;
            case PATIENCE -> normalized;
            case INVENTORY -> 45.0 * normalized;
        };
        return finalizeValue(ability, value);
    }

    public static double finalizeEffective(Ability ability, double value) {
        Objects.requireNonNull(ability, "ability");
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("value must be finite and >= 0");
        }
        return finalizeValue(ability, value);
    }

    private static double finalizeValue(Ability ability, double value) {
        if (ability == Ability.HEALTH) {
            return Math.round(value);
        }
        if (ability == Ability.INVENTORY) {
            return Math.min(45.0, Math.round(value));
        }
        return value;
    }
}
