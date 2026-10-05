package co.surumene.www.ability;

import co.surumene.www.domain.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PersonalityAbilityModifier {
    private PersonalityAbilityModifier() {}

    public static double multiplier(
            Personality personality,
            Ability ability,
            List<ExpressedTrait> traits,
            double configuredRate) {
        Objects.requireNonNull(personality, "personality");
        Objects.requireNonNull(ability, "ability");
        Objects.requireNonNull(traits, "traits");
        if (!Double.isFinite(configuredRate) || configuredRate < 0.0) {
            throw new IllegalArgumentException("configuredRate must be finite and >= 0");
        }

        double rate = configuredRate * (hasQuirkBoost(traits) ? 1.5 : 1.0);
        Targets targets = targets(personality);
        if (targets.positive().contains(ability)) return 1.0 + rate;
        if (targets.negative().contains(ability)) return Math.max(0.0, 1.0 - rate);
        return 1.0;
    }

    public static Set<Ability> positive(Personality personality) {
        return Set.copyOf(targets(Objects.requireNonNull(personality, "personality")).positive());
    }

    public static Set<Ability> negative(Personality personality) {
        return Set.copyOf(targets(Objects.requireNonNull(personality, "personality")).negative());
    }

    private static boolean hasQuirkBoost(List<ExpressedTrait> traits) {
        return traits.stream().anyMatch(entry -> entry.trait() == Trait.QUIRK_BOOST);
    }

    private static Targets targets(Personality personality) {
        return switch (personality) {
            case SERIOUS -> targets();
            case HASTY -> targets(set(Ability.MOVEMENT_SPEED), set(Ability.INVENTORY));
            case JOLLY -> targets(set(Ability.ATTACK_SPEED), set(Ability.DEFENSE));
            case NIMBLE -> targets(set(Ability.MOVEMENT_SPEED), set(Ability.ATTACK_DAMAGE));
            case VALIANT -> targets(set(Ability.ATTACK_SPEED), set(Ability.INVENTORY));
            case HARD_WORKING -> targets(set(Ability.INVENTORY), set(Ability.ATTACK_SPEED));
            case NAUGHTY -> targets(set(Ability.JUMP), set(Ability.PATIENCE));
            case STURDY -> targets(set(Ability.STEP_HEIGHT), set(Ability.ATTACK_SPEED));
            case ADAMANT -> targets(set(Ability.PATIENCE), set(Ability.INVENTORY));
            case RELAXED -> targets(set(Ability.INVENTORY), set(Ability.MOVEMENT_SPEED));
            case CAUTIOUS -> targets(set(Ability.DEFENSE), set(Ability.ATTACK_DAMAGE));
            case MIGHTY -> targets(set(Ability.ATTACK_DAMAGE), set(Ability.MOVEMENT_SPEED));
            case GENTLE -> targets(set(Ability.HEALTH), set(Ability.ATTACK_SPEED));
            case ROWDY -> targets(set(Ability.ATTACK_DAMAGE), set(Ability.DEFENSE));
            case BRAVE -> targets(set(Ability.PATIENCE), set(Ability.MOVEMENT_SPEED));
            case POWERFUL -> targets(set(Ability.INVENTORY), set(Ability.MOVEMENT_SPEED));
            case GLUTTONOUS -> targets(set(Ability.INVENTORY, Ability.SIZE), set());
        };
    }

    private static EnumSet<Ability> set(Ability... abilities) {
        if (abilities.length == 0) return EnumSet.noneOf(Ability.class);
        return EnumSet.of(abilities[0], abilities);
    }

    private static Targets targets() {
        return new Targets(EnumSet.noneOf(Ability.class), EnumSet.noneOf(Ability.class));
    }

    private static Targets targets(EnumSet<Ability> positive, EnumSet<Ability> negative) {
        return new Targets(positive, negative);
    }

    private record Targets(EnumSet<Ability> positive, EnumSet<Ability> negative) {}
}
