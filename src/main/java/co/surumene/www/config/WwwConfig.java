package co.surumene.www.config;

public record WwwConfig(
        int configVersion,
        FounderTarget founderTarget,
        GenomeProfile genomeProfile,
        Runtime runtime) {

    public record Distribution(double mean, double standardDeviation) {}
    public record FounderKind(Distribution abilities) {}
    public record FounderTarget(
            FounderKind natural,
            FounderKind wolfTrap,
            Distribution personality,
            Distribution development,
            Distribution relationship) {}

    public record GenomeProfile(
            Decoder decoder,
            Synthesizer synthesizer,
            BreedingPolicy breedingPolicy) {}

    public record Decoder(
            PersonalityDecoder personality,
            TraitDecoder trait,
            InjuryDecoder injury,
            DivineDecoder divine) {}

    public record PersonalityDecoder(
            double seriousMaxScore,
            double seriousSpread,
            double dominantGap) {}

    public record TraitDecoder(double expressionThreshold, double strongGap) {}

    public record InjuryDecoder(
            double expressionThreshold,
            double onsetMaxGameDays,
            double severityRankMin,
            double severityRankMax) {}

    public record DivineDecoder(double minHaplotypeScore, double totalScore) {}

    public record Synthesizer(
            InjurySynthesizer injury,
            DivineSynthesizer divine,
            ExtraordinarySynthesizer extraordinary,
            double cancellationMin,
            double cancellationMax,
            double highTargetHeadroom,
            double personalityCancellationMin,
            double personalityCancellationMax,
            GenesPerTarget genesPerTarget,
            double chromosomeLengthStandardDeviationRatio,
            double chromosomeLengthMinRatio,
            double chromosomeLengthMaxRatio,
            int directGenesSoftMin,
            int directGenesSoftMax,
            int directGenesHardMax,
            int regulationGenesMin,
            int regulationGenesCenter,
            int regulationGenesMax,
            int regulationGenesHardMax,
            int recognizableGenesHardMax,
            double recognizableRegionMaxRatio,
            double noncodingRegionMinRatio,
            Relay relay) {}

    public record InjurySynthesizer(
            double blocksLambda,
            int blocksMax,
            int genesPerBlock,
            double loadMean,
            double loadStandardDeviation,
            double loadMin,
            double loadMax,
            double bilateralProbability,
            double onsetMean,
            double onsetStandardDeviation,
            double severityMean,
            double severityStandardDeviation) {}

    public record DivineSynthesizer(
            double supplyProbability,
            int genesMin,
            int genesCenter,
            int genesMax,
            double majorHaplotypeProbability,
            double geneDMin,
            double geneDMax) {}

    public record ExtraordinarySynthesizer(
            int genesPerTargetMin,
            int genesPerTargetMax,
            int blocksPerTargetMin,
            int blocksPerTargetMax,
            double transferMax) {}

    public record GenesPerTarget(
            Range ability,
            Range personality,
            Range development,
            Range relationship,
            Range trait) {}

    public record Range(int min, int center, int max) {}

    public record Relay(
            double attachmentMinRatio,
            double attachmentMaxRatio,
            double normalSecondaryMinRatio,
            double normalSecondaryMaxRatio,
            double strongSecondaryMinRatio,
            double strongSecondaryMaxRatio,
            double positiveRatio) {}

    public record BreedingPolicy(
            DirectInheritance directInheritance,
            WildTrait wildTrait) {}

    public record DirectInheritance(
            double weakPreferProbability,
            double crossoverWeightInsideBlock) {}

    public record WildTrait(
            double weakParentMultiplier,
            double strongParentMultiplier) {}

    public record Runtime(
            Combat combat,
            ActionDistance actionDistance,
            WanWand wanWand,
            Age age,
            Relationship relationship,
            PersonalityRuntime personality,
            Traits traits,
            Spawn spawn) {}

    public record Combat(int retreatMinTicks) {}

    public record ActionDistance(
            double narrowBlocks,
            double normalBlocks,
            double wideBlocks,
            double veryWideBlocks) {}

    public record WanWand(double targetMaxDistanceBlocks) {}

    public record Age(
            String clockWorld,
            double baseGrowthGameDays,
            double basePeakDurationGameDays,
            double baseAgingDurationGameDays,
            AgeSensitivity sensitivity) {}

    public record AgeSensitivity(
            double health,
            double defense,
            double patience,
            double size,
            double inventory,
            double movementSpeed,
            double jump,
            double stepHeight,
            double attackDamage,
            double attackSpeed) {}

    public record Relationship(
            int passiveIncreaseIntervalTicks,
            double passiveIncreaseProbability) {}

    public record PersonalityRuntime(double modifierRate) {}

    public record Traits(
            double auraRadiusBlocks,
            double watchmanDistanceMultiplier,
            int watchmanWarningMinTicks,
            int watchmanWarningMaxTicks) {}

    public record Spawn(
            double naturalConversionProbability,
            double wolfTrapLightningProbability,
            double wolfTrapActivationRadiusBlocks) {}
}
