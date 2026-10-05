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
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            personality.put(factor, random.nextDouble());
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
        int first = random.nextInt(TRAIT_CANDIDATE_COUNT);
        int second = random.nextInt(TRAIT_CANDIDATE_COUNT);
        int none = Trait.values().length;

        if (first == none && second == none) {
            return List.of();
        }
        if (first == second) {
            return List.of(new ExpressedTrait(Trait.values()[first], TraitStrength.STRONG));
        }

        List<ExpressedTrait> out = new ArrayList<>(2);
        if (first != none) {
            out.add(new ExpressedTrait(Trait.values()[first], TraitStrength.WEAK));
        }
        if (second != none) {
            out.add(new ExpressedTrait(Trait.values()[second], TraitStrength.WEAK));
        }
        out.sort(Comparator.comparingInt(entry -> entry.trait().targetId()));
        return List.copyOf(out);
    }

    private static double truncatedNormal(
            double mean,
            double standardDeviation,
            double min,
            double max,
            GenomeRandom random) {
        for (;;) {
            double u1 = random.nextDouble();
            double u2 = random.nextDouble();
            if (!(u1 > 0.0)) {
                continue;
            }
            double z = StrictMath.sqrt(-2.0 * StrictMath.log(u1))
                    * StrictMath.cos(2.0 * StrictMath.PI * u2);
            double value = mean + standardDeviation * z;
            if (value >= min && value <= max) {
                return value;
            }
        }
    }
}
