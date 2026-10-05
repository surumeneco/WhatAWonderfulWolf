package co.surumene.www.founder;

import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.Trait;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FounderTarget(
        FounderOrigin origin,
        Map<Ability, Double> abilities,
        Map<PersonalityFactor, Double> personalityFactors,
        List<ExpressedTrait> traits,
        Map<DevelopmentFactor, Double> developmentFactors,
        FounderRelationshipTarget relationship) {

    public FounderTarget {
        Objects.requireNonNull(origin, "origin");
        abilities = completeScores(Ability.class, abilities, 0.0, 1.5, "abilities");
        personalityFactors = completeScores(
                PersonalityFactor.class, personalityFactors, 0.0, 1.0, "personalityFactors");
        developmentFactors = completeScores(
                DevelopmentFactor.class, developmentFactors, 0.0, 1.0, "developmentFactors");
        traits = List.copyOf(Objects.requireNonNull(traits, "traits"));
        if (traits.size() > 2) {
            throw new IllegalArgumentException("traits must contain at most two entries");
        }
        HashSet<Trait> unique = new HashSet<>();
        for (ExpressedTrait trait : traits) {
            Objects.requireNonNull(trait, "trait");
            if (!unique.add(trait.trait())) {
                throw new IllegalArgumentException("traits must not contain duplicates");
            }
        }
        Objects.requireNonNull(relationship, "relationship");
    }

    private static <K extends Enum<K>> Map<K, Double> completeScores(
            Class<K> type, Map<K, Double> source, double min, double max, String name) {
        Objects.requireNonNull(source, name);
        EnumMap<K, Double> copy = new EnumMap<>(type);
        for (K key : type.getEnumConstants()) {
            Double value = source.get(key);
            if (value == null || !Double.isFinite(value) || value < min || value > max) {
                throw new IllegalArgumentException(name + " has invalid or missing value for " + key);
            }
            copy.put(key, value);
        }
        if (copy.size() != source.size()) {
            throw new IllegalArgumentException(name + " contains unexpected entries");
        }
        return Collections.unmodifiableMap(copy);
    }
}
