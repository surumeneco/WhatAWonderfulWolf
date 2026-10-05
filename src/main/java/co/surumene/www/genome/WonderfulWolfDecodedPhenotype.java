package co.surumene.www.genome;

import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.InjuryPhenotype;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.domain.RelationshipPerformance;
import co.surumene.wgl.api.DecoderIdentity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record WonderfulWolfDecodedPhenotype(
        Map<Ability, Double> baseAbilities,
        Map<Ability, Double> extraordinaryContributions,
        RelationshipPerformance relationshipPerformance,
        Map<PersonalityFactor, Double> personalityFactors,
        Personality personality,
        List<ExpressedTrait> expressedTraits,
        Map<DevelopmentFactor, Double> developmentFactors,
        List<InjuryPhenotype> injuries,
        DivineLineagePhenotype divineLineage) {

    public WonderfulWolfDecodedPhenotype {
        baseAbilities = immutableEnumMap(Ability.class, baseAbilities);
        extraordinaryContributions = immutableEnumMap(Ability.class, extraordinaryContributions);
        personalityFactors = immutableEnumMap(PersonalityFactor.class, personalityFactors);
        developmentFactors = immutableEnumMap(DevelopmentFactor.class, developmentFactors);
        relationshipPerformance = Objects.requireNonNull(relationshipPerformance, "relationshipPerformance");
        personality = Objects.requireNonNull(personality, "personality");
        expressedTraits = List.copyOf(Objects.requireNonNull(expressedTraits, "expressedTraits"));
        injuries = List.copyOf(Objects.requireNonNull(injuries, "injuries"));
        divineLineage = Objects.requireNonNull(divineLineage, "divineLineage");
    }

    public PhenotypeSnapshot toSnapshot(DecoderIdentity identity, PhenotypeOrigin origin) {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(origin, "origin");
        boolean extraordinaryActive =
                origin == PhenotypeOrigin.WOLF_TRAP_FOUNDER || divineLineage.expressed();

        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            double base = baseAbilities.get(ability);
            double extraordinary = extraordinaryActive ? extraordinaryContributions.get(ability) : 0.0;
            abilities.put(ability, Math.min(1.5, base + extraordinary));
        }

        return new PhenotypeSnapshot(
                identity,
                abilities,
                relationshipPerformance,
                personalityFactors,
                personality,
                expressedTraits,
                developmentFactors,
                injuries,
                divineLineage.expressed());
    }

    private static <K extends Enum<K>> Map<K, Double> immutableEnumMap(
            Class<K> type, Map<K, Double> source) {
        Objects.requireNonNull(source, "source");
        EnumMap<K, Double> copy = new EnumMap<>(type);
        copy.putAll(source);
        for (K key : type.getEnumConstants()) {
            Double value = copy.get(key);
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("missing or non-finite value for " + key);
            }
        }
        if (copy.size() != type.getEnumConstants().length) {
            throw new IllegalArgumentException("unexpected enum-map entries");
        }
        return java.util.Collections.unmodifiableMap(copy);
    }
}
