package co.surumene.www.genome;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.RelationshipPerformance;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.BackboneDefinition;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.DirectContributionModel;
import co.surumene.wgl.api.EffectiveContribution;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeProfile;
import co.surumene.wgl.api.ProfileDescriptor;
import co.surumene.wgl.api.StandardDirectContributionModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public final class WonderfulWolfGenomeProfile implements GenomeProfile<WonderfulWolfDecodedPhenotype> {
    static final double EPS = 1.0e-12;

    private static final DirectContributionModel STANDARD_CONTRIBUTION =
            StandardDirectContributionModel.defaultModel();
    private static final DirectContributionModel EXTRAORDINARY_CONTRIBUTION =
            new StandardDirectContributionModel(
                    StandardDirectContributionModel.DEFAULT_ALPHA,
                    2.0,
                    address -> 2.0);

    private final WwwConfig config;
    private final ProfileDescriptor descriptor;
    private final BackboneDefinition backbone;

    public WonderfulWolfGenomeProfile(WwwConfig config) {
        this.config = WwwConfigValidator.validate(Objects.requireNonNull(config, "config"));
        this.descriptor = WonderfulWolfProfileFoundation.descriptor(this.config);
        this.backbone = WonderfulWolfProfileFoundation.backbone(this.config);
    }

    public WwwConfig config() {
        return config;
    }

    public BackboneDefinition backbone() {
        return backbone;
    }

    @Override
    public ProfileDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public boolean isDefinedAddress(GenomeAddress address) {
        Objects.requireNonNull(address, "address");
        return switch (address.type()) {
            case 0x00 -> address.target() <= 0x09;
            case 0x01 -> address.target() <= 0x04;
            case 0x02 -> address.target() <= 0x09;
            case 0x03 -> address.target() <= 0x05;
            case 0x04 -> address.target() <= 0x11;
            case 0x05 -> address.target() == 0x00;
            case 0x06 -> address.target() <= 0x01;
            case 0x07 -> address.target() <= 0x09;
            default -> false;
        };
    }

    @Override
    public int minimumExtensionBits(GenomeAddress address) {
        Objects.requireNonNull(address, "address");
        return address.type() == 0x02 && address.target() <= 0x09 ? 16 : 0;
    }

    @Override
    public DirectContributionModel contributionModel(GenomeAddress address) {
        Objects.requireNonNull(address, "address");
        if (!isDefinedAddress(address)) {
            throw new IllegalArgumentException("undefined Wonderful Wolf address: " + address);
        }
        return address.type() == 0x07 ? EXTRAORDINARY_CONTRIBUTION : STANDARD_CONTRIBUTION;
    }

    @Override
    public WonderfulWolfDecodedPhenotype mapPhenotype(DecodedGenome decodedGenome) {
        Objects.requireNonNull(decodedGenome, "decodedGenome");

        EnumMap<Ability, Double> baseAbilities = new EnumMap<>(Ability.class);
        EnumMap<Ability, Double> extraordinary = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            baseAbilities.put(
                    ability,
                    decodedGenome.aggregate(new GenomeAddress(0x00, ability.targetId())).score());
            extraordinary.put(
                    ability,
                    0.5 * decodedGenome.aggregate(new GenomeAddress(0x07, ability.targetId())).score());
        }

        EnumMap<PersonalityFactor, Double> personalityFactors = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            personalityFactors.put(
                    factor,
                    centeredScore(decodedGenome.aggregate(new GenomeAddress(0x03, factor.targetId()))));
        }

        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            development.put(
                    factor,
                    centeredScore(decodedGenome.aggregate(new GenomeAddress(0x01, factor.targetId()))));
        }

        RelationshipPerformance relationship = new RelationshipPerformance(
                (int) Math.round(-100.0 + 200.0 * centeredScore(
                        decodedGenome.aggregate(new GenomeAddress(0x06, 0x00)))),
                (int) Math.round(5.0 + 10.0 * centeredScore(
                        decodedGenome.aggregate(new GenomeAddress(0x06, 0x01)))));

        DivineLineagePhenotype divine = decodeDivine(
                decodedGenome.aggregate(new GenomeAddress(0x05, 0x00)));

        return new WonderfulWolfDecodedPhenotype(
                baseAbilities,
                extraordinary,
                relationship,
                personalityFactors,
                decodePersonality(personalityFactors),
                decodeTraits(decodedGenome),
                development,
                List.of(),
                divine);
    }

    private Personality decodePersonality(EnumMap<PersonalityFactor, Double> scores) {
        WwwConfig.PersonalityDecoder decoder = config.genomeProfile().decoder().personality();
        double neutralFactorDistance = decoder.neutralFactorSigma() * decoder.sigma();
        double neutralSpread = decoder.neutralSpreadSigma() * decoder.sigma();

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        boolean allNearMean = true;
        for (double score : scores.values()) {
            min = Math.min(min, score);
            max = Math.max(max, score);
            if (Math.abs(score - decoder.mean()) > neutralFactorDistance + EPS) {
                allNearMean = false;
            }
        }
        if (allNearMean && max - min <= neutralSpread + EPS) {
            return Personality.SERIOUS;
        }

        List<PersonalityFactor> ranked = new ArrayList<>(List.of(PersonalityFactor.values()));
        ranked.sort(Comparator
                .comparingDouble((PersonalityFactor factor) -> scores.get(factor))
                .reversed()
                .thenComparingInt(PersonalityFactor::targetId));

        PersonalityFactor first = ranked.get(0);
        PersonalityFactor second = ranked.get(1);
        double dominantGap = decoder.dominantGapSigma() * decoder.sigma();
        if (scores.get(first) - scores.get(second) >= dominantGap - EPS) {
            second = first;
        }
        return personalityForPair(first, second);
    }

    private static Personality personalityForPair(PersonalityFactor first, PersonalityFactor second) {
        int a = Math.min(first.targetId(), second.targetId());
        int b = Math.max(first.targetId(), second.targetId());
        return switch ((a << 8) | b) {
            case 0x0000, 0x0005 -> Personality.HASTY;
            case 0x0001 -> Personality.JOLLY;
            case 0x0002 -> Personality.NIMBLE;
            case 0x0003 -> Personality.VALIANT;
            case 0x0004 -> Personality.HARD_WORKING;
            case 0x0101, 0x0105 -> Personality.NAUGHTY;
            case 0x0102 -> Personality.STURDY;
            case 0x0103 -> Personality.ADAMANT;
            case 0x0104 -> Personality.RELAXED;
            case 0x0202, 0x0205 -> Personality.CAUTIOUS;
            case 0x0203 -> Personality.MIGHTY;
            case 0x0204 -> Personality.GENTLE;
            case 0x0303, 0x0305 -> Personality.ROWDY;
            case 0x0304 -> Personality.BRAVE;
            case 0x0404, 0x0405 -> Personality.POWERFUL;
            case 0x0505 -> Personality.GLUTTONOUS;
            default -> throw new IllegalStateException("unsupported personality factor pair: " + first + "/" + second);
        };
    }

    private List<ExpressedTrait> decodeTraits(DecodedGenome decodedGenome) {
        WwwConfig.TraitDecoder decoder = config.genomeProfile().decoder().trait();
        List<TraitScore> candidates = new ArrayList<>();
        for (Trait trait : Trait.values()) {
            double score = decodedGenome.aggregate(new GenomeAddress(0x04, trait.targetId())).score();
            if (score >= decoder.expressionThreshold() - EPS) {
                candidates.add(new TraitScore(trait, score));
            }
        }
        candidates.sort(Comparator
                .comparingDouble(TraitScore::score)
                .reversed()
                .thenComparingInt(entry -> entry.trait().targetId()));

        if (candidates.isEmpty()) {
            return List.of();
        }
        if (candidates.size() == 1) {
            return List.of(new ExpressedTrait(candidates.getFirst().trait(), TraitStrength.WEAK));
        }

        TraitScore first = candidates.get(0);
        TraitScore second = candidates.get(1);
        if (first.score() - second.score() >= decoder.strongGap() - EPS) {
            return List.of(new ExpressedTrait(first.trait(), TraitStrength.STRONG));
        }
        return List.of(
                new ExpressedTrait(first.trait(), TraitStrength.WEAK),
                new ExpressedTrait(second.trait(), TraitStrength.WEAK));
    }

    static double centeredScore(AddressAggregate aggregate) {
        double negative = 1.0 - aggregate.negativeSurvival();
        return clamp01(0.5 + 0.5 * (aggregate.positiveSaturation() - negative));
    }

    private DivineLineagePhenotype decodeDivine(AddressAggregate aggregate) {
        double a = haplotypeScore(aggregate.contributions(), 0);
        double b = haplotypeScore(aggregate.contributions(), 1);
        WwwConfig.DivineDecoder decoder = config.genomeProfile().decoder().divine();
        boolean expressed =
                Math.min(a, b) >= decoder.minHaplotypeScore() - EPS
                        && a + b >= decoder.totalScore() - EPS;
        return new DivineLineagePhenotype(a, b, expressed);
    }

    static double haplotypeScore(List<EffectiveContribution> contributions, int haplotype) {
        double positiveSurvival = 1.0;
        double negativeSurvival = 1.0;
        for (EffectiveContribution contribution : contributions) {
            if (contribution.haplotypeIndex() != haplotype) {
                continue;
            }
            if (contribution.effect() >= 0.0) {
                positiveSurvival *= 1.0 - contribution.saturation();
            } else {
                negativeSurvival *= 1.0 - contribution.saturation();
            }
        }
        return (1.0 - positiveSurvival) * negativeSurvival;
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record TraitScore(Trait trait, double score) {}
}
