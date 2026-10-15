package co.surumene.www.domain;

import co.surumene.wgl.api.DecoderIdentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PhenotypeSnapshot(
        DecoderIdentity decoderIdentity,
        Map<Ability, Double> abilities,
        RelationshipPerformance relationshipPerformance,
        Map<PersonalityFactor, Double> personalityFactors,
        Personality personality,
        List<ExpressedTrait> expressedTraits,
        Map<DevelopmentFactor, Double> developmentFactors,
        List<InjuryPhenotype> injuries,
        double divineLineageTotalScore,
        boolean divineLineageExpressed) {

    public PhenotypeSnapshot {
        Objects.requireNonNull(decoderIdentity, "decoderIdentity");
        Objects.requireNonNull(relationshipPerformance, "relationshipPerformance");
        Objects.requireNonNull(personality, "personality");
        if (!Double.isFinite(divineLineageTotalScore)
                || divineLineageTotalScore < 0.0
                || divineLineageTotalScore > 2.0) {
            throw new IllegalArgumentException("divineLineageTotalScore must be finite and in [0, 2]");
        }

        abilities = copyCompleteScores(Ability.class, abilities, 0.0, 1.5, "abilities");
        personalityFactors = copyCompleteScores(
                PersonalityFactor.class, personalityFactors, 0.0, 1.0, "personalityFactors");
        developmentFactors = copyCompleteScores(
                DevelopmentFactor.class, developmentFactors, 0.0, 1.0, "developmentFactors");

        expressedTraits = List.copyOf(Objects.requireNonNull(expressedTraits, "expressedTraits"));
        if (expressedTraits.size() > 2) {
            throw new IllegalArgumentException("expressedTraits must contain at most two traits");
        }
        HashSet<Trait> traitKinds = new HashSet<>();
        for (ExpressedTrait trait : expressedTraits) {
            Objects.requireNonNull(trait, "expressedTrait");
            if (!traitKinds.add(trait.trait())) {
                throw new IllegalArgumentException("expressedTraits must not contain duplicate traits");
            }
        }

        injuries = List.copyOf(Objects.requireNonNull(injuries, "injuries"));
        EnumSet<Ability> injuredAbilities = EnumSet.noneOf(Ability.class);
        for (InjuryPhenotype injury : injuries) {
            Objects.requireNonNull(injury, "injury");
            if (!injuredAbilities.add(injury.ability())) {
                throw new IllegalArgumentException("injuries must contain at most one entry per ability");
            }
        }
    }

    private static <K extends Enum<K>> Map<K, Double> copyCompleteScores(
            Class<K> enumType,
            Map<K, Double> source,
            double min,
            double max,
            String name) {
        Objects.requireNonNull(source, name);
        EnumMap<K, Double> copy = new EnumMap<>(enumType);
        for (K key : enumType.getEnumConstants()) {
            Double value = source.get(key);
            if (value == null) {
                throw new IllegalArgumentException(name + " is missing " + key);
            }
            if (!Double.isFinite(value) || value < min || value > max) {
                throw new IllegalArgumentException(name + "[" + key + "] must be finite and in [" + min + ", " + max + "]");
            }
            copy.put(key, value);
        }
        if (source.size() != copy.size()) {
            throw new IllegalArgumentException(name + " contains unexpected entries");
        }
        return Collections.unmodifiableMap(copy);
    }
}
