package co.surumene.www.runtime;

import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;

import java.util.EnumSet;
import java.util.Objects;

public final class EffectPolicy {
    private static final EnumSet<Trait> LEVELLED_EFFECTS = EnumSet.of(
            Trait.MINING_SUPPORT,
            Trait.MUSCLE,
            Trait.GUARDIAN,
            Trait.HEALING,
            Trait.HOLY_POISON,
            Trait.UNYIELDING,
            Trait.LIFE_DRAIN,
            Trait.INTIMIDATION);

    private EffectPolicy() {}

    public enum DamageKind {
        FALL,
        DROWNING,
        FREEZING,
        OTHER
    }

    public static int amplifier(
            Trait trait,
            TraitStrength strength) {
        Objects.requireNonNull(trait, "trait");
        Objects.requireNonNull(strength, "strength");
        return strength == TraitStrength.STRONG
                        && LEVELLED_EFFECTS.contains(trait)
                ? 1
                : 0;
    }

    public static boolean immuneTo(
            Trait trait,
            DamageKind kind) {
        Objects.requireNonNull(trait, "trait");
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case FALL -> trait == Trait.LIGHTWEIGHT;
            case DROWNING -> trait == Trait.SWIMMER;
            case FREEZING -> trait == Trait.SNOWBORN;
            case OTHER -> false;
        };
    }

    public static long watchmanInterval(
            double distance,
            double radius,
            int minimumTicks,
            int maximumTicks) {
        if (!Double.isFinite(distance) || distance < 0.0) {
            throw new IllegalArgumentException(
                    "distance must be finite and >= 0");
        }
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException(
                    "radius must be finite and >= 0");
        }
        if (minimumTicks < 0 || maximumTicks < minimumTicks) {
            throw new IllegalArgumentException(
                    "warning ticks must satisfy 0 <= min <= max");
        }

        if (radius == 0.0) {
            return distance == 0.0 ? minimumTicks : maximumTicks;
        }
        double proportion = Math.max(
                0.0,
                Math.min(1.0, distance / radius));
        return Math.round(
                minimumTicks
                        + proportion * (maximumTicks - minimumTicks));
    }

    public static boolean shouldApplyPotion(
            int currentAmplifier,
            int currentDurationTicks,
            int desiredAmplifier,
            int refreshThresholdTicks) {
        if (currentDurationTicks < 0 || refreshThresholdTicks < 0) {
            throw new IllegalArgumentException(
                    "durations must be >= 0");
        }
        return currentAmplifier < desiredAmplifier
                || (currentAmplifier == desiredAmplifier
                    && currentDurationTicks <= refreshThresholdTicks);
    }
}
