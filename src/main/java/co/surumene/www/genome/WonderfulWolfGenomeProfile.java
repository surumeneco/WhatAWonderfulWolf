package co.surumene.www.genome;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.InjuryPhenotype;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.domain.RelationshipPerformance;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.founder.WonderfulWolfSynthesisTarget;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.BackboneDefinition;
import co.surumene.wgl.api.DecodedGene;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.DecodedHomologyBlock;
import co.surumene.wgl.api.DirectContributionModel;
import co.surumene.wgl.api.EffectiveContribution;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeProfile;
import co.surumene.wgl.api.BitSequence;
import co.surumene.wgl.api.ChromosomeTemplate;
import co.surumene.wgl.api.AnchorSeed;
import co.surumene.wgl.api.ProfileDescriptor;
import co.surumene.wgl.api.StandardDirectContributionModel;
import co.surumene.wgl.api.GenomeRandom;
import co.surumene.wgl.api.GeneSequenceCodec;
import co.surumene.wgl.api.SynthesisAddressPlan;
import co.surumene.wgl.api.SynthesisContext;
import co.surumene.wgl.api.SynthesisBlock;
import co.surumene.wgl.api.SynthesisSafetyPolicy;
import co.surumene.wgl.api.SynthesisTarget;

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
    private final GeneSequenceCodec geneSequenceCodec;

    public WonderfulWolfGenomeProfile(WwwConfig config) {
        this(config, null);
    }

    public WonderfulWolfGenomeProfile(WwwConfig config, GeneSequenceCodec geneSequenceCodec) {
        this.config = WwwConfigValidator.validate(Objects.requireNonNull(config, "config"));
        this.descriptor = WonderfulWolfProfileFoundation.descriptor(this.config);
        this.backbone = WonderfulWolfProfileFoundation.backbone(this.config);
        this.geneSequenceCodec = geneSequenceCodec;
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
    public BitSequence founderTemplateBits(
            int chromosomeIndex,
            int haplotypeIndex,
            ChromosomeTemplate template,
            GenomeRandom random) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(random, "random");

        BitSequence varied = template.templateBits();
        if (template.markerLocus() == null) {
            return varied;
        }

        varied = varyFounderMarkerAnchor(
                varied,
                template.markerLocus().first(),
                random.nextInt(49));
        varied = varyFounderMarkerAnchor(
                varied,
                template.markerLocus().second(),
                random.nextInt(49));
        return varied;
    }

    private static BitSequence varyFounderMarkerAnchor(
            BitSequence bits,
            AnchorSeed anchor,
            int choice) {
        if (choice < 0 || choice > 48) {
            throw new IllegalArgumentException("marker variation choice must be in [0,48]");
        }
        return choice == 0
                ? bits
                : bits.flip(anchor.position() + choice - 1);
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
    public boolean requiresHomologyContext() {
        return true;
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
    public SynthesisAddressPlan synthesisPlan(
            GenomeAddress address,
            double target,
            SynthesisContext context,
            GenomeRandom random) {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(random, "random");
        if (!isDefinedAddress(address)) {
            throw new IllegalArgumentException("undefined Wonderful Wolf address: " + address);
        }

        WwwConfig.Synthesizer synth = config.genomeProfile().synthesizer();
        return switch (address.type()) {
            case 0x00 -> boundedPlan(target, synth.genesPerTarget().ability(), synth, random);
            case 0x01 -> centeredPlan(target, synth.genesPerTarget().development(), random);
            case 0x03 -> personalityPlan(
                    target,
                    synth.genesPerTarget().personality(),
                    synth,
                    random);
            case 0x04 -> boundedPlan(target, synth.genesPerTarget().trait(), synth, random);
            case 0x06 -> centeredPlan(target, synth.genesPerTarget().relationship(), random);
            case 0x07 -> boundedPlan(
                    target,
                    new WwwConfig.Range(
                            synth.extraordinary().genesPerTargetMin(),
                            (synth.extraordinary().genesPerTargetMin()
                                    + synth.extraordinary().genesPerTargetMax()) / 2,
                            synth.extraordinary().genesPerTargetMax()),
                    synth,
                    random);
            default -> GenomeProfile.super.synthesisPlan(address, target, context, random);
        };
    }

    private static SynthesisAddressPlan centeredPlan(
            double target,
            WwwConfig.Range perHaplotypeRange,
            GenomeRandom random) {
        int totalGenes = 2 * triangularInt(
                perHaplotypeRange.min(),
                perHaplotypeRange.center(),
                perHaplotypeRange.max(),
                random);
        if (target == 0.5) {
            return new SynthesisAddressPlan(0.0, 0.0, 0, 0, 0, 0);
        }
        double delta = 2.0 * target - 1.0;
        if (delta > 0.0) {
            return new SynthesisAddressPlan(delta, 0.0, totalGenes, totalGenes, 0, 0);
        }
        return new SynthesisAddressPlan(0.0, -delta, 0, 0, totalGenes, totalGenes);
    }

    private static SynthesisAddressPlan personalityPlan(
            double target,
            WwwConfig.Range perHaplotypeRange,
            WwwConfig.Synthesizer synth,
            GenomeRandom random) {
        if (!Double.isFinite(target) || target < 0.0 || target > 1.0) {
            throw new IllegalArgumentException("target must be finite and in [0,1]");
        }

        int totalGenes = 2 * triangularInt(
                perHaplotypeRange.min(),
                perHaplotypeRange.center(),
                perHaplotypeRange.max(),
                random);
        double delta = 2.0 * target - 1.0;
        double draw = synth.personalityCancellationMin()
                + (synth.personalityCancellationMax()
                    - synth.personalityCancellationMin()) * random.nextDouble();
        double cancellation = Math.min(draw, Math.max(0.0, 1.0 - Math.abs(delta)));
        double positive = Math.max(0.0, delta) + cancellation;
        double negative = Math.max(0.0, -delta) + cancellation;

        if (positive == 0.0) {
            return new SynthesisAddressPlan(
                    0.0, negative,
                    0, 0,
                    totalGenes, totalGenes);
        }
        if (negative == 0.0) {
            return new SynthesisAddressPlan(
                    positive, 0.0,
                    totalGenes, totalGenes,
                    0, 0);
        }

        int positiveGenes = (int) StrictMath.round(
                totalGenes * positive / (positive + negative));
        positiveGenes = Math.max(1, Math.min(totalGenes - 1, positiveGenes));
        int negativeGenes = totalGenes - positiveGenes;
        return new SynthesisAddressPlan(
                positive,
                negative,
                positiveGenes,
                positiveGenes,
                negativeGenes,
                negativeGenes);
    }

    private static SynthesisAddressPlan boundedPlan(
            double target,
            WwwConfig.Range perHaplotypeRange,
            WwwConfig.Synthesizer synth,
            GenomeRandom random) {
        if (!Double.isFinite(target) || target < 0.0 || target > 1.0) {
            throw new IllegalArgumentException("target must be finite and in [0,1]");
        }
        if (target == 0.0) {
            return new SynthesisAddressPlan(0.0, 0.0, 0, 0, 0, 0);
        }

        double draw = synth.cancellationMin()
                + (synth.cancellationMax() - synth.cancellationMin()) * random.nextDouble();
        double cancellation = Math.min(
                draw,
                Math.max(0.0, 1.0 - synth.highTargetHeadroom() - target));
        double survival = 1.0 - cancellation;
        double positive = target / survival;

        int totalGenes = 2 * triangularInt(
                perHaplotypeRange.min(),
                perHaplotypeRange.center(),
                perHaplotypeRange.max(),
                random);
        if (cancellation == 0.0) {
            return new SynthesisAddressPlan(
                    positive, 0.0,
                    totalGenes, totalGenes,
                    0, 0);
        }

        double negativeShare = cancellation / (positive + cancellation);
        int negativeGenes = Math.max(1,
                Math.min(totalGenes - 1,
                        (int) StrictMath.round(totalGenes * negativeShare)));
        int positiveGenes = totalGenes - negativeGenes;
        return new SynthesisAddressPlan(
                positive,
                cancellation,
                positiveGenes,
                positiveGenes,
                negativeGenes,
                negativeGenes);
    }

    private static int triangularInt(
            int min,
            int mode,
            int max,
            GenomeRandom random) {
        if (min == max) return min;
        double u = random.nextDouble();
        double split = (mode - min) / (double) (max - min);
        double value = u < split
                ? min + StrictMath.sqrt(u * (max - min) * (mode - min))
                : max - StrictMath.sqrt((1.0 - u) * (max - min) * (max - mode));
        return Math.max(min, Math.min(max, (int) StrictMath.round(value)));
    }

    @Override
    public List<SynthesisBlock> synthesisBlocks(
            SynthesisTarget target,
            SynthesisContext context,
            GenomeRandom random) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(random, "random");
        if (!(target instanceof WonderfulWolfSynthesisTarget wonderfulWolfTarget)) {
            return List.of();
        }
        if (geneSequenceCodec == null) {
            throw new IllegalStateException(
                    "Wonderful Wolf synthesis requires the WGL GeneSequenceCodec");
        }
        return new WonderfulWolfSynthesisMaterial(
                config,
                backbone,
                geneSequenceCodec,
                this::contributionModel)
                .blocks(wonderfulWolfTarget, random);
    }

    @Override
    public SynthesisSafetyPolicy synthesisSafetyPolicy() {
        WwwConfig.Synthesizer synth = config.genomeProfile().synthesizer();
        return (metrics, decodedGenome) -> {
            for (int haplotype = 0; haplotype <= 1; haplotype++) {
                int lane = haplotype;
                int candidateCount = metrics.haplotypes().stream()
                        .filter(metric -> metric.haplotypeIndex() == lane)
                        .mapToInt(metric -> metric.geneCandidateCount())
                        .sum();
                long recognizableBits = metrics.haplotypes().stream()
                        .filter(metric -> metric.haplotypeIndex() == lane)
                        .mapToLong(metric -> metric.recognizableBits())
                        .sum();
                long totalBits = metrics.haplotypes().stream()
                        .filter(metric -> metric.haplotypeIndex() == lane)
                        .mapToLong(metric -> metric.bitLength())
                        .sum();
                double recognizableRatio =
                        totalBits == 0L ? 0.0 : recognizableBits / (double) totalBits;

                long direct = decodedGenome.physicalGenes().stream()
                        .filter(DecodedGene::addressValid)
                        .filter(gene -> gene.haplotypeIndex() == lane)
                        .filter(gene -> !gene.regulation())
                        .count();
                long regulation = decodedGenome.physicalGenes().stream()
                        .filter(DecodedGene::addressValid)
                        .filter(gene -> gene.haplotypeIndex() == lane)
                        .filter(DecodedGene::regulation)
                        .count();

                if (candidateCount > synth.recognizableGenesHardMax()
                        || direct > synth.directGenesHardMax()
                        || regulation > synth.regulationGenesHardMax()
                        || recognizableRatio > synth.recognizableRegionMaxRatio() + EPS
                        || 1.0 - recognizableRatio < synth.noncodingRegionMinRatio() - EPS) {
                    return false;
                }
            }
            return true;
        };
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
                decodeInjuries(decodedGenome),
                divine);
    }

    private Personality decodePersonality(EnumMap<PersonalityFactor, Double> scores) {
        WwwConfig.PersonalityDecoder decoder =
                config.genomeProfile().decoder().personality();

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double score : scores.values()) {
            min = Math.min(min, score);
            max = Math.max(max, score);
        }
        if (max < decoder.seriousMaxScore()
                || max - min <= decoder.seriousSpread() + EPS) {
            return Personality.SERIOUS;
        }

        List<PersonalityFactor> ranked =
                new ArrayList<>(List.of(PersonalityFactor.values()));
        ranked.sort(Comparator
                .comparingDouble((PersonalityFactor factor) -> scores.get(factor))
                .reversed()
                .thenComparingInt(PersonalityFactor::targetId));

        PersonalityFactor first = ranked.get(0);
        PersonalityFactor second = ranked.get(1);
        if (scores.get(first) - scores.get(second)
                >= decoder.dominantGap() - EPS) {
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

    private List<InjuryPhenotype> decodeInjuries(DecodedGenome decodedGenome) {
        List<InjuryPhenotype> injuries = new ArrayList<>();
        WwwConfig.InjuryDecoder decoder = config.genomeProfile().decoder().injury();

        for (Ability ability : Ability.values()) {
            GenomeAddress address = new GenomeAddress(0x02, ability.targetId());
            List<EffectiveContribution> contributions = decodedGenome.aggregate(address).contributions();
            if (contributions.isEmpty()) {
                continue;
            }

            List<InjuryPair> pairs = new ArrayList<>();
            double injurySurvival = 1.0;
            for (DecodedHomologyBlock block : decodedGenome.homologyBlocks()) {
                List<EffectiveContribution> sideA = injuryContributions(contributions, block, 0);
                List<EffectiveContribution> sideB = injuryContributions(contributions, block, 1);
                double loadA = boundedScore(sideA);
                double loadB = boundedScore(sideB);
                if (loadA <= 0.0 || loadB <= 0.0) {
                    continue;
                }

                List<DecodedGene> genes = injuryGenes(decodedGenome.physicalGenes(), address, block);
                if (genes.isEmpty()) {
                    continue;
                }

                double pairLoad = StrictMath.sqrt(loadA * loadB);
                injurySurvival *= 1.0 - pairLoad;
                double onset = genes.stream()
                        .mapToDouble(gene -> gene.extension().toLong(0, 8) / 255.0)
                        .average()
                        .orElseThrow();
                double severity = genes.stream()
                        .mapToDouble(gene -> gene.extension().toLong(8, 8) / 255.0)
                        .average()
                        .orElseThrow();
                pairs.add(new InjuryPair(pairLoad, onset, severity));
            }

            double score = 1.0 - injurySurvival;
            if (score < decoder.expressionThreshold() - EPS || pairs.isEmpty()) {
                continue;
            }

            double totalWeight = pairs.stream().mapToDouble(InjuryPair::weight).sum();
            double onsetNormalized = pairs.stream()
                    .mapToDouble(pair -> pair.weight() * pair.onsetNormalized())
                    .sum() / totalWeight;
            double severityNormalized = pairs.stream()
                    .mapToDouble(pair -> pair.weight() * pair.severityNormalized())
                    .sum() / totalWeight;

            double onsetGameDay = Math.round(decoder.onsetMaxGameDays() * onsetNormalized);
            double severityRank = decoder.severityRankMin()
                    + (decoder.severityRankMax() - decoder.severityRankMin()) * severityNormalized;
            injuries.add(new InjuryPhenotype(ability, onsetGameDay, severityRank));
        }

        return List.copyOf(injuries);
    }

    private static List<EffectiveContribution> injuryContributions(
            List<EffectiveContribution> contributions,
            DecodedHomologyBlock block,
            int haplotype) {
        int start = haplotype == 0 ? block.startA() : block.startB();
        int end = haplotype == 0 ? block.endAExclusive() : block.endBExclusive();
        return contributions.stream()
                .filter(contribution -> !contribution.secondary())
                .filter(contribution -> contribution.chromosomeIndex() == block.chromosomeIndex())
                .filter(contribution -> contribution.haplotypeIndex() == haplotype)
                .filter(contribution -> contribution.startBit() >= start && contribution.startBit() < end)
                .toList();
    }

    private static List<DecodedGene> injuryGenes(
            List<DecodedGene> genes,
            GenomeAddress address,
            DecodedHomologyBlock block) {
        return genes.stream()
                .filter(DecodedGene::addressValid)
                .filter(gene -> address.equals(gene.address()))
                .filter(gene -> gene.extension().bitLength() >= 16)
                .filter(gene -> gene.chromosomeIndex() == block.chromosomeIndex())
                .filter(gene -> {
                    int start = gene.haplotypeIndex() == 0 ? block.startA() : block.startB();
                    int end = gene.haplotypeIndex() == 0 ? block.endAExclusive() : block.endBExclusive();
                    return gene.haplotypeIndex() >= 0
                            && gene.startBit() >= start
                            && gene.startBit() < end;
                })
                .toList();
    }

    private static double boundedScore(List<EffectiveContribution> contributions) {
        double positiveSurvival = 1.0;
        double negativeSurvival = 1.0;
        for (EffectiveContribution contribution : contributions) {
            if (contribution.effect() >= 0.0) {
                positiveSurvival *= 1.0 - contribution.saturation();
            } else {
                negativeSurvival *= 1.0 - contribution.saturation();
            }
        }
        return (1.0 - positiveSurvival) * negativeSurvival;
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

    private record InjuryPair(
            double weight,
            double onsetNormalized,
            double severityNormalized) {}
}
