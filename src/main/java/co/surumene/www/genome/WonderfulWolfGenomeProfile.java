package co.surumene.www.genome;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.RelationshipPerformance;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.BackboneDefinition;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.DirectContributionModel;
import co.surumene.wgl.api.EffectiveContribution;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeProfile;
import co.surumene.wgl.api.ProfileDescriptor;
import co.surumene.wgl.api.StandardDirectContributionModel;

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
                Personality.SERIOUS,
                List.of(),
                development,
                List.of(),
                divine);
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
}
