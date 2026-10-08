package co.surumene.www.config;

import java.util.Objects;

public final class WwwConfigValidator {
    private WwwConfigValidator() {}

    public static WwwConfig validate(WwwConfig config) {
        Objects.requireNonNull(config, "config");
        if (config.configVersion() != 1) {
            throw invalid("config-version", "must be 1");
        }

        validateDistribution(config.founderTarget().natural().abilities(), "founder-target.natural.abilities");
        validateDistribution(config.founderTarget().wolfTrap().abilities(), "founder-target.wolf-trap.abilities");
        validateDistribution(config.founderTarget().personality(), "founder-target.personality");
        validateDistribution(config.founderTarget().development(), "founder-target.development");
        validateDistribution(config.founderTarget().relationship(), "founder-target.relationship");

        validateDecoder(config.genomeProfile().decoder());
        validateSynthesizer(config.genomeProfile().synthesizer());
        validateBreedingPolicy(config.genomeProfile().breedingPolicy());
        validateRuntime(config.runtime());
        return config;
    }

    private static void validateDistribution(WwwConfig.Distribution value, String path) {
        finite(value.mean(), path + ".mean");
        positive(value.standardDeviation(), path + ".standard-deviation");
    }

    private static void validateDecoder(WwwConfig.Decoder decoder) {
        WwwConfig.PersonalityDecoder personality = decoder.personality();
        unit(personality.seriousMaxScore(), "genome-profile.decoder.personality.serious-max-score");
        unit(personality.seriousSpread(), "genome-profile.decoder.personality.serious-spread");
        unit(personality.dominantGap(), "genome-profile.decoder.personality.dominant-gap");

        WwwConfig.TraitDecoder trait = decoder.trait();
        unit(trait.expressionThreshold(), "genome-profile.decoder.trait.expression-threshold");
        unit(trait.strongGap(), "genome-profile.decoder.trait.strong-gap");
        double threshold = trait.expressionThreshold();
        double strongGap = trait.strongGap();
        double margin = Math.max(0.01, strongGap * 0.25);
        // Keep both inactive scores below the threshold and the strong Founder
        // score above its runner-up by the configured gap without saturation.
        if (!(threshold > 0.0 && strongGap > 0.0
                && threshold + strongGap + 2.0 * margin <= 1.0)) {
            throw invalid("genome-profile.decoder.trait",
                    "must satisfy threshold > 0, strong-gap > 0, and "
                            + "threshold + strong-gap + 2 * max(0.01, strong-gap * 0.25) <= 1");
        }

        WwwConfig.InjuryDecoder injury = decoder.injury();
        unit(injury.expressionThreshold(), "genome-profile.decoder.injury.expression-threshold");
        positive(injury.onsetMaxGameDays(), "genome-profile.decoder.injury.onset-max-game-days");
        nonNegative(injury.severityRankMin(), "genome-profile.decoder.injury.severity-rank-min");
        finite(injury.severityRankMax(), "genome-profile.decoder.injury.severity-rank-max");
        ordered(injury.severityRankMin(), injury.severityRankMax(),
                "genome-profile.decoder.injury.severity-rank-min",
                "genome-profile.decoder.injury.severity-rank-max");

        WwwConfig.DivineDecoder divine = decoder.divine();
        unit(divine.minHaplotypeScore(), "genome-profile.decoder.divine.min-haplotype-score");
        unit(divine.totalScore(), "genome-profile.decoder.divine.total-score");
    }

    private static void validateSynthesizer(WwwConfig.Synthesizer synth) {
        WwwConfig.InjurySynthesizer injury = synth.injury();
        nonNegative(injury.blocksLambda(), "genome-profile.synthesizer.injury.blocks-lambda");
        atLeastOne(injury.blocksMax(), "genome-profile.synthesizer.injury.blocks-max");
        atLeastOne(injury.genesPerBlock(), "genome-profile.synthesizer.injury.genes-per-block");
        unit(injury.loadMean(), "genome-profile.synthesizer.injury.load-mean");
        positive(injury.loadStandardDeviation(), "genome-profile.synthesizer.injury.load-standard-deviation");
        unit(injury.loadMin(), "genome-profile.synthesizer.injury.load-min");
        unit(injury.loadMax(), "genome-profile.synthesizer.injury.load-max");
        if (!(injury.loadMin() <= injury.loadMean() && injury.loadMean() <= injury.loadMax())) {
            throw invalid("genome-profile.synthesizer.injury.load-*", "must satisfy min <= mean <= max");
        }
        probability(injury.bilateralProbability(), "genome-profile.synthesizer.injury.bilateral-probability");
        unit(injury.onsetMean(), "genome-profile.synthesizer.injury.onset-mean");
        positive(injury.onsetStandardDeviation(), "genome-profile.synthesizer.injury.onset-standard-deviation");
        unit(injury.severityMean(), "genome-profile.synthesizer.injury.severity-mean");
        positive(injury.severityStandardDeviation(), "genome-profile.synthesizer.injury.severity-standard-deviation");

        WwwConfig.DivineSynthesizer divine = synth.divine();
        probability(divine.supplyProbability(), "genome-profile.synthesizer.divine.supply-probability");
        orderedRange(divine.genesMin(), divine.genesCenter(), divine.genesMax(),
                "genome-profile.synthesizer.divine.genes");
        probability(divine.majorHaplotypeProbability(), "genome-profile.synthesizer.divine.major-haplotype-probability");
        nonNegative(divine.geneDMin(), "genome-profile.synthesizer.divine.gene-d-min");
        finite(divine.geneDMax(), "genome-profile.synthesizer.divine.gene-d-max");
        ordered(divine.geneDMin(), divine.geneDMax(),
                "genome-profile.synthesizer.divine.gene-d-min",
                "genome-profile.synthesizer.divine.gene-d-max");

        WwwConfig.ExtraordinarySynthesizer extraordinary = synth.extraordinary();
        positiveRange(extraordinary.genesPerTargetMin(), extraordinary.genesPerTargetMax(),
                "genome-profile.synthesizer.extraordinary.genes-per-target");
        positiveRange(extraordinary.blocksPerTargetMin(), extraordinary.blocksPerTargetMax(),
                "genome-profile.synthesizer.extraordinary.blocks-per-target");
        unit(extraordinary.transferMax(), "genome-profile.synthesizer.extraordinary.transfer-max");

        unit(synth.cancellationMin(), "genome-profile.synthesizer.cancellation-min");
        unit(synth.cancellationMax(), "genome-profile.synthesizer.cancellation-max");
        ordered(synth.cancellationMin(), synth.cancellationMax(),
                "genome-profile.synthesizer.cancellation-min",
                "genome-profile.synthesizer.cancellation-max");
        unit(synth.highTargetHeadroom(), "genome-profile.synthesizer.high-target-headroom");
        unit(synth.personalityCancellationMin(),
                "genome-profile.synthesizer.personality-cancellation-min");
        unit(synth.personalityCancellationMax(),
                "genome-profile.synthesizer.personality-cancellation-max");
        ordered(
                synth.personalityCancellationMin(),
                synth.personalityCancellationMax(),
                "genome-profile.synthesizer.personality-cancellation-min",
                "genome-profile.synthesizer.personality-cancellation-max");

        validateRange(synth.genesPerTarget().ability(), "genome-profile.synthesizer.genes-per-target.ability");
        validateRange(synth.genesPerTarget().personality(), "genome-profile.synthesizer.genes-per-target.personality");
        validateRange(synth.genesPerTarget().development(), "genome-profile.synthesizer.genes-per-target.development");
        validateRange(synth.genesPerTarget().relationship(), "genome-profile.synthesizer.genes-per-target.relationship");
        validateRange(synth.genesPerTarget().trait(), "genome-profile.synthesizer.genes-per-target.trait");

        positive(synth.chromosomeLengthStandardDeviationRatio(),
                "genome-profile.synthesizer.chromosome-length-standard-deviation-ratio");
        positive(synth.chromosomeLengthMinRatio(),
                "genome-profile.synthesizer.chromosome-length-min-ratio");
        finite(synth.chromosomeLengthMaxRatio(),
                "genome-profile.synthesizer.chromosome-length-max-ratio");
        if (!(synth.chromosomeLengthMinRatio() <= 1.0 && synth.chromosomeLengthMaxRatio() >= 1.0
                && synth.chromosomeLengthMinRatio() <= synth.chromosomeLengthMaxRatio())) {
            throw invalid("genome-profile.synthesizer.chromosome-length-*",
                    "must satisfy 0 < min <= 1 <= max");
        }

        if (!(synth.directGenesSoftMin() >= 1
                && synth.directGenesSoftMin() <= synth.directGenesSoftMax()
                && synth.directGenesSoftMax() <= synth.directGenesHardMax())) {
            throw invalid("genome-profile.synthesizer.direct-genes-*",
                    "must satisfy 1 <= soft-min <= soft-max <= hard-max");
        }
        if (!(synth.regulationGenesMin() >= 1
                && synth.regulationGenesMin() <= synth.regulationGenesCenter()
                && synth.regulationGenesCenter() <= synth.regulationGenesMax()
                && synth.regulationGenesMax() <= synth.regulationGenesHardMax())) {
            throw invalid("genome-profile.synthesizer.regulation-genes-*",
                    "must satisfy 1 <= min <= center <= max <= hard-max");
        }
        atLeastOne(synth.recognizableGenesHardMax(), "genome-profile.synthesizer.recognizable-genes-hard-max");

        unit(synth.recognizableRegionMaxRatio(), "genome-profile.synthesizer.recognizable-region-max-ratio");
        unit(synth.noncodingRegionMinRatio(), "genome-profile.synthesizer.noncoding-region-min-ratio");
        if (synth.recognizableRegionMaxRatio() + synth.noncodingRegionMinRatio() > 1.0) {
            throw invalid("genome-profile.synthesizer.*-region-*-ratio", "sum must be <= 1");
        }

        WwwConfig.Relay relay = synth.relay();
        ratioRange(relay.attachmentMinRatio(), relay.attachmentMaxRatio(),
                "genome-profile.synthesizer.relay.attachment");
        ratioRange(relay.normalSecondaryMinRatio(), relay.normalSecondaryMaxRatio(),
                "genome-profile.synthesizer.relay.normal-secondary");
        ratioRange(relay.strongSecondaryMinRatio(), relay.strongSecondaryMaxRatio(),
                "genome-profile.synthesizer.relay.strong-secondary");
        probability(relay.positiveRatio(), "genome-profile.synthesizer.relay.positive-ratio");
    }

    private static void validateBreedingPolicy(WwwConfig.BreedingPolicy policy) {
        probability(policy.directInheritance().weakPreferProbability(),
                "genome-profile.breeding-policy.direct-inheritance.weak-prefer-probability");
        unit(policy.directInheritance().crossoverWeightInsideBlock(),
                "genome-profile.breeding-policy.direct-inheritance.crossover-weight-inside-block");
        positive(policy.wildTrait().weakParentMultiplier(),
                "genome-profile.breeding-policy.wild-trait.weak-parent-multiplier");
        positive(policy.wildTrait().strongParentMultiplier(),
                "genome-profile.breeding-policy.wild-trait.strong-parent-multiplier");
    }

    private static void validateRuntime(WwwConfig.Runtime runtime) {
        nonNegative(runtime.combat().retreatMinTicks(), "runtime.combat.retreat-min-ticks");

        WwwConfig.ActionDistance distance = runtime.actionDistance();
        nonNegative(distance.narrowBlocks(), "runtime.action-distance.narrow-blocks");
        finite(distance.normalBlocks(), "runtime.action-distance.normal-blocks");
        finite(distance.wideBlocks(), "runtime.action-distance.wide-blocks");
        finite(distance.veryWideBlocks(), "runtime.action-distance.very-wide-blocks");
        if (!(distance.narrowBlocks() <= distance.normalBlocks()
                && distance.normalBlocks() <= distance.wideBlocks()
                && distance.wideBlocks() <= distance.veryWideBlocks())) {
            throw invalid("runtime.action-distance", "must satisfy narrow <= normal <= wide <= very-wide");
        }

        positive(runtime.wanWand().targetMaxDistanceBlocks(), "runtime.wan-wand.target-max-distance-blocks");

        WwwConfig.Age age = runtime.age();
        if (age.clockWorld() == null || age.clockWorld().isBlank()) {
            throw invalid("runtime.age.clock-world", "must not be blank");
        }
        positive(age.baseGrowthGameDays(), "runtime.age.base-growth-game-days");
        nonNegative(age.basePeakDurationGameDays(), "runtime.age.base-peak-duration-game-days");
        positive(age.baseAgingDurationGameDays(), "runtime.age.base-aging-duration-game-days");
        WwwConfig.AgeSensitivity sensitivity = age.sensitivity();
        nonNegative(sensitivity.health(), "runtime.age.sensitivity.health");
        nonNegative(sensitivity.defense(), "runtime.age.sensitivity.defense");
        nonNegative(sensitivity.patience(), "runtime.age.sensitivity.patience");
        nonNegative(sensitivity.size(), "runtime.age.sensitivity.size");
        nonNegative(sensitivity.inventory(), "runtime.age.sensitivity.inventory");
        nonNegative(sensitivity.movementSpeed(), "runtime.age.sensitivity.movement-speed");
        nonNegative(sensitivity.jump(), "runtime.age.sensitivity.jump");
        nonNegative(sensitivity.stepHeight(), "runtime.age.sensitivity.step-height");
        nonNegative(sensitivity.attackDamage(), "runtime.age.sensitivity.attack-damage");
        nonNegative(sensitivity.attackSpeed(), "runtime.age.sensitivity.attack-speed");

        atLeastOne(runtime.relationship().passiveIncreaseIntervalTicks(),
                "runtime.relationship.passive-increase-interval-ticks");
        probability(runtime.relationship().passiveIncreaseProbability(),
                "runtime.relationship.passive-increase-probability");
        nonNegative(runtime.personality().modifierRate(), "runtime.personality.modifier-rate");

        nonNegative(runtime.traits().auraRadiusBlocks(), "runtime.traits.aura-radius-blocks");
        nonNegative(runtime.traits().watchmanDistanceMultiplier(), "runtime.traits.watchman-distance-multiplier");
        nonNegative(runtime.traits().watchmanWarningMinTicks(), "runtime.traits.watchman-warning-min-ticks");
        nonNegative(runtime.traits().watchmanWarningMaxTicks(), "runtime.traits.watchman-warning-max-ticks");
        if (runtime.traits().watchmanWarningMinTicks() > runtime.traits().watchmanWarningMaxTicks()) {
            throw invalid("runtime.traits.watchman-warning-*", "must satisfy min <= max");
        }

        probability(runtime.spawn().naturalConversionProbability(), "runtime.spawn.natural-conversion-probability");
        probability(runtime.spawn().wolfTrapLightningProbability(), "runtime.spawn.wolf-trap-lightning-probability");
        nonNegative(runtime.spawn().wolfTrapActivationRadiusBlocks(), "runtime.spawn.wolf-trap-activation-radius-blocks");
    }

    private static void validateRange(WwwConfig.Range range, String path) {
        orderedRange(range.min(), range.center(), range.max(), path);
    }

    private static void orderedRange(int min, int center, int max, String path) {
        if (!(min >= 1 && min <= center && center <= max)) {
            throw invalid(path, "must satisfy 1 <= min <= center <= max");
        }
    }

    private static void positiveRange(int min, int max, String path) {
        if (!(min >= 1 && min <= max)) {
            throw invalid(path, "must satisfy 1 <= min <= max");
        }
    }

    private static void ratioRange(double min, double max, String path) {
        unit(min, path + "-min-ratio");
        unit(max, path + "-max-ratio");
        ordered(min, max, path + "-min-ratio", path + "-max-ratio");
    }

    private static void probability(double value, String path) {
        unit(value, path);
    }

    private static void unit(double value, String path) {
        finite(value, path);
        if (value < 0.0 || value > 1.0) {
            throw invalid(path, "must be in [0, 1]");
        }
    }

    private static void positive(double value, String path) {
        finite(value, path);
        if (value <= 0.0) {
            throw invalid(path, "must be > 0");
        }
    }

    private static void nonNegative(double value, String path) {
        finite(value, path);
        if (value < 0.0) {
            throw invalid(path, "must be >= 0");
        }
    }

    private static void nonNegative(int value, String path) {
        if (value < 0) {
            throw invalid(path, "must be >= 0");
        }
    }

    private static void atLeastOne(int value, String path) {
        if (value < 1) {
            throw invalid(path, "must be >= 1");
        }
    }

    private static void finite(double value, String path) {
        if (!Double.isFinite(value)) {
            throw invalid(path, "must be finite");
        }
    }

    private static void ordered(double min, double max, String minPath, String maxPath) {
        if (min > max) {
            throw invalid(minPath + "/" + maxPath, "must satisfy min <= max");
        }
    }

    private static IllegalArgumentException invalid(String path, String message) {
        return new IllegalArgumentException(path + " " + message);
    }
}
