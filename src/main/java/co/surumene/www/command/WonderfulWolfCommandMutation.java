package co.surumene.www.command;

import co.surumene.www.ability.AbilityScale;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
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

    public static double toNormalized(Ability ability, double canonical) {
        Objects.requireNonNull(ability, "ability");
        if (!Double.isFinite(canonical)) {
            throw new IllegalArgumentException("value must be finite");
        }
        double value = switch (ability) {
            case HEALTH -> (Math.round(canonical) - 20.0) / 40.0;
            case SIZE -> (canonical - 1.5) / 1.5;
            case MOVEMENT_SPEED -> (canonical - 3.0) / 21.0;
            case JUMP -> (canonical - 1.0) / 4.0;
            case STEP_HEIGHT -> canonical - 0.5;
            case ATTACK_DAMAGE -> (canonical - 1.0) / 9.0;
            case ATTACK_SPEED -> (canonical - 0.2) / 1.8;
            case DEFENSE -> canonical / 30.0;
            case PATIENCE -> canonical;
            case INVENTORY -> Math.round(canonical) / 30.0;
        };
        if (value < -1e-10 || value > 1.5 + 1e-10) {
            throw new IllegalArgumentException(
                    ability + " must be in the canonical range "
                            + AbilityScale.toCanonical(ability, 0.0) + ".."
                            + AbilityScale.toCanonical(ability, 1.5));
        }
        return Math.max(0.0, Math.min(1.5, value));
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
                original.pedigree());
    }

    public static WonderfulWolfIndividual withAbility(
            WonderfulWolfIndividual original, Ability ability, String operation, double operand) {
        Objects.requireNonNull(original, "original");
        if (!operation.equals("set") && !operation.equals("add")) {
            throw new IllegalArgumentException("operation must be set or add");
        }
        double current = AbilityScale.toCanonical(
                ability, original.phenotypeSnapshot().abilities().get(ability));
        double result = operation.equals("add") ? current + operand : operand;
        double normalized = toNormalized(ability, result);
        PhenotypeSnapshot old = original.phenotypeSnapshot();
        EnumMap<Ability, Double> scores = new EnumMap<>(Ability.class);
        scores.putAll(old.abilities());
        scores.put(ability, normalized);
        PhenotypeSnapshot updated = new PhenotypeSnapshot(
                old.decoderIdentity(),
                scores,
                old.relationshipPerformance(),
                old.personalityFactors(),
                old.personality(),
                old.expressedTraits(),
                old.developmentFactors(),
                old.injuries(),
                old.divineLineageTotalScore(),
                old.divineLineageExpressed());
        return new WonderfulWolfIndividual(
                original.genome(),
                updated,
                original.ownerId(),
                original.adultBiologicalTime(),
                original.mode(),
                original.commanderId(),
                original.actionDistance(),
                original.waitLocation(),
                original.affection(),
                original.weapon(),
                original.inventory(),
                original.generation(),
                original.pedigree());
    }
}
