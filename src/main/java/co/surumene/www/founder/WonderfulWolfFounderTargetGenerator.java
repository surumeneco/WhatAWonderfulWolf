package co.surumene.www.founder;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.wgl.api.GenomeRandom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public final class WonderfulWolfFounderTargetGenerator {
    private static final int TRAIT_CANDIDATE_COUNT = Trait.values().length + 1;

    private final WwwConfig config;

    public WonderfulWolfFounderTargetGenerator(WwwConfig config) {
        this.config = WwwConfigValidator.validate(Objects.requireNonNull(config, "config"));
    }

    public FounderTarget generate(FounderOrigin origin, GenomeRandom random) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(random, "random");

        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        WwwConfig.Distribution abilityDistribution = switch (origin) {
            case NATURAL -> config.founderTarget().natural().abilities();
            case WOLF_TRAP -> config.founderTarget().wolfTrap().abilities();
        };
        double abilityMin = origin == FounderOrigin.NATURAL ? 0.0 : 0.5;
        double abilityMax = origin == FounderOrigin.NATURAL ? 0.5 : 1.5;
        for (Ability ability : Ability.values()) {
            abilities.put(ability, truncatedNormal(
                    abilityDistribution.mean(),
                    abilityDistribution.standardDeviation(),
                    abilityMin,
                    abilityMax,
                    random));
        }

        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        WwwConfig.Distribution personalityDistribution =
                config.founderTarget().personality();
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            personality.put(factor, truncatedNormal(
                    personalityDistribution.mean(),
                    personalityDistribution.standardDeviation(),
                    0.0,
                    1.0,
                    random));
        }

        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        WwwConfig.Distribution developmentDistribution = config.founderTarget().development();
        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            development.put(factor, truncatedNormal(
                    developmentDistribution.mean(),
                    developmentDistribution.standardDeviation(),
                    0.0,
                    1.0,
                    random));
        }

        WwwConfig.Distribution relationshipDistribution = config.founderTarget().relationship();
        FounderRelationshipTarget relationship = new FounderRelationshipTarget(
                truncatedNormal(
                        relationshipDistribution.mean(),
                        relationshipDistribution.standardDeviation(),
                        0.0,
                        1.0,
                        random),
                truncatedNormal(
                        relationshipDistribution.mean(),
                        relationshipDistribution.standardDeviation(),
                        0.0,
                        1.0,
                        random));

        return new FounderTarget(
                origin,
                abilities,
                personality,
                drawTraits(random),
                development,
                relationship);
    }

    private static List<ExpressedTrait> drawTraits(GenomeRandom random) {
        return co.surumene.wcl.api.genetics.FounderSampling.drawTraits(
                        List.of(Trait.values()), Trait::targetId, random::nextInt)
                .stream()
                .map(choice -> new ExpressedTrait(choice.trait(),
                        choice.strong() ? TraitStrength.STRONG : TraitStrength.WEAK))
                .toList();
    }

    private static double truncatedNormal(
            double mean, double standardDeviation, double min, double max,
            GenomeRandom random) {
        return co.surumene.wcl.api.genetics.FounderSampling.truncatedNormal(
                mean, standardDeviation, min, max, random::nextDouble);
    }
}
