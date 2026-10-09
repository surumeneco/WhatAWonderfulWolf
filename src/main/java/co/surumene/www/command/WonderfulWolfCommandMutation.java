package co.surumene.www.command;

import co.surumene.www.ability.AbilityScale;
import co.surumene.www.domain.Ability;
import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit admin edits affect the stored phenotype snapshot, not the inherited
 * genome. Ordinary breeding continues to use the underlying genome.
 */
public final class WonderfulWolfCommandMutation {
    private WonderfulWolfCommandMutation() {}

    public static Optional<Ability> ability(String raw) {
        String key = Objects.requireNonNull(raw, "field")
                .trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (key) {
            case "health", "max-health" -> Optional.of(Ability.HEALTH);
            case "size", "scale" -> Optional.of(Ability.SIZE);
            case "speed", "movement-speed", "ground-speed" -> Optional.of(Ability.MOVEMENT_SPEED);
            case "jump", "jump-strength" -> Optional.of(Ability.JUMP);
            case "step-height" -> Optional.of(Ability.STEP_HEIGHT);
            case "attack-damage", "damage" -> Optional.of(Ability.ATTACK_DAMAGE);
            case "attack-speed" -> Optional.of(Ability.ATTACK_SPEED);
            case "defense", "armor" -> Optional.of(Ability.DEFENSE);
            case "patience" -> Optional.of(Ability.PATIENCE);
            case "inventory", "capacity", "cargo" -> Optional.of(Ability.INVENTORY);
            default -> Optional.empty();
        };
    }

    /** Returns an unbounded normalized projection for display/ranking only. */
    public static double toNormalized(Ability ability, double canonical) {
        Objects.requireNonNull(ability, "ability");
        double value = validateCanonical(ability, canonical);
        double low = AbilityScale.toCanonical(ability, 0.0);
        double span = AbilityScale.toCanonical(ability, 1.0) - low;
        return (value - low) / span;
    }

    /** Validate input without imposing the natural Founder/Genome score ceiling. */
    public static double validateCanonical(Ability ability, double canonical) {
        Objects.requireNonNull(ability, "ability");
        if (!Double.isFinite(canonical)) {
            throw new IllegalArgumentException("value must be finite");
        }
        double value = switch (ability) {
            case HEALTH, INVENTORY -> Math.round(canonical);
            default -> canonical;
        };
        if ((ability == Ability.HEALTH || ability == Ability.SIZE) && value <= 0.0) {
            throw new IllegalArgumentException(ability + " must be > 0");
        }
        if (value < 0.0) {
            throw new IllegalArgumentException(ability + " must be >= 0");
        }
        return value;
    }

    public static WonderfulWolfIndividual withOwner(
            WonderfulWolfIndividual original, java.util.UUID ownerId) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(ownerId, "ownerId");
        return new WonderfulWolfIndividual(
                original.genome(),
                original.phenotypeSnapshot(),
                Optional.of(ownerId),
                original.adultBiologicalTime(),
                original.mode(),
                original.commanderId(),
                original.actionDistance(),
                original.waitLocation(),
                original.affection(),
                original.weapon(),
                original.inventory(),
                original.generation(),
                original.pedigree(),
                original.adminAbilityOverrides());
    }

    public static WonderfulWolfIndividual withAbility(
            WonderfulWolfIndividual original, Ability ability, String operation,
            double operand) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(ability, "ability");
        if (!operation.equals("set") && !operation.equals("add")) {
            throw new IllegalArgumentException("operation must be set or add");
        }
        if (!Double.isFinite(operand)) {
            throw new IllegalArgumentException("operand must be finite");
        }
        double current = original.adminAbilityOverrides().getOrDefault(ability,
                AbilityScale.toCanonical(ability,
                        original.phenotypeSnapshot().abilities().get(ability)));
        double result = operation.equals("add") ? current + operand : operand;
        double validated = validateCanonical(ability, result);
        EnumMap<Ability, Double> overrides = new EnumMap<>(Ability.class);
        overrides.putAll(original.adminAbilityOverrides());
        overrides.put(ability, validated);
        return original.withAdminAbilityOverrides(overrides);
    }
}
