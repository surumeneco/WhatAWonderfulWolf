package co.surumene.www.breeding;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wgl.api.*;

import java.util.*;
import java.util.function.ToDoubleFunction;

public final class WonderfulWolfBreedingContextFactory {
    private final WonderfulWolfGenomeProfile profile;
    private final WonderfulWolfInheritanceAnalyzer analyzer;

    public WonderfulWolfBreedingContextFactory(
            GenomeEngine engine,
            WonderfulWolfGenomeProfile profile) {
        Objects.requireNonNull(engine, "engine");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.analyzer = new WonderfulWolfInheritanceAnalyzer(engine, profile);
    }

    public BreedingContext create(
            WonderfulWolfIndividual parentA,
            WonderfulWolfIndividual parentB,
            GenomeRandom random) {
        Objects.requireNonNull(parentA, "parentA");
        Objects.requireNonNull(parentB, "parentB");
        Objects.requireNonNull(random, "random");

        WwwConfig.BreedingPolicy policy =
                profile.config().genomeProfile().breedingPolicy();
        WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisA =
                analyzer.analyze(parentA);
        WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisB =
                analyzer.analyze(parentB);

        HardSelection hard = selectHard(
                parentA, analysisA,
                parentB, analysisB,
                random);

        Optional<TraitStrength> directA =
                WonderfulWolfBreedingPolicy.expressedTrait(
                        parentA.phenotypeSnapshot(), Trait.DIRECT_INHERITANCE);
        Optional<TraitStrength> directB =
                WonderfulWolfBreedingPolicy.expressedTrait(
                        parentB.phenotypeSnapshot(), Trait.DIRECT_INHERITANCE);

        SoftSelection soft = selectSoft(
                analysisA, directA,
                analysisB, directB,
                hard.parentA(), hard.parentB(),
                policy, random);

        List<InheritanceConstraint> finalA = combine(
                hard.parentA(),
                WonderfulWolfBreedingPolicy.resolveSoftAgainstHard(
                        hard.parentA(), soft.parentA()));
        List<InheritanceConstraint> finalB = combine(
                hard.parentB(),
                WonderfulWolfBreedingPolicy.resolveSoftAgainstHard(
                        hard.parentB(), soft.parentB()));

        return new BreedingContext(
                profile.backbone(),
                WonderfulWolfBreedingPolicy.mutationMultiplier(
                        parentA.phenotypeSnapshot(),
                        parentB.phenotypeSnapshot(),
                        policy),
                WonderfulWolfBreedingPolicy.deNovoForbiddenAddresses(),
                null,
                false,
                new ParentMeiosisPolicy(finalA),
                new ParentMeiosisPolicy(finalB));
    }

    private HardSelection selectHard(
            WonderfulWolfIndividual parentA,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisA,
            WonderfulWolfIndividual parentB,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisB,
            GenomeRandom random) {
        List<AbilityWeight> weightsA = hardAbilityWeights(parentA);
        List<AbilityWeight> weightsB = hardAbilityWeights(parentB);

        if (weightsA.isEmpty() && weightsB.isEmpty()) {
            return HardSelection.empty();
        }
        if (weightsA.isEmpty()) {
            return hardSingle(analysisB, weightsB, random)
                    .map(choice -> new HardSelection(
                            List.of(),
                            List.of(choice.block().hardConstraint())))
                    .orElseGet(HardSelection::empty);
        }
        if (weightsB.isEmpty()) {
            return hardSingle(analysisA, weightsA, random)
                    .map(choice -> new HardSelection(
                            List.of(choice.block().hardConstraint()),
                            List.of()))
                    .orElseGet(HardSelection::empty);
        }

        List<AbilityPair> pairs = distinctPairs(weightsA, weightsB);
        while (!pairs.isEmpty()) {
            AbilityPair selected =
                    chooseWeighted(pairs, AbilityPair::weight, random);
            Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blockA =
                    bestExtraordinaryBlock(analysisA, selected.a().ability());
            Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blockB =
                    bestExtraordinaryBlock(analysisB, selected.b().ability());

            if (blockA.isPresent() && blockB.isPresent()) {
                return new HardSelection(
                        List.of(blockA.orElseThrow().hardConstraint()),
                        List.of(blockB.orElseThrow().hardConstraint()));
            }

            if (blockA.isEmpty()) {
                pairs = pairs.stream()
                        .filter(pair -> pair.a().ability() != selected.a().ability())
                        .toList();
            }
            if (blockB.isEmpty()) {
                pairs = pairs.stream()
                        .filter(pair -> pair.b().ability() != selected.b().ability())
                        .toList();
            }
        }

        Optional<HardChoice> singleA =
                hardSingle(analysisA, weightsA, random);
        Optional<HardChoice> singleB =
                hardSingle(analysisB, weightsB, random);
        if (singleA.isEmpty() && singleB.isEmpty()) {
            return HardSelection.empty();
        }
        if (singleA.isEmpty()) {
            return new HardSelection(
                    List.of(),
                    List.of(singleB.orElseThrow().block().hardConstraint()));
        }
        if (singleB.isEmpty()) {
            return new HardSelection(
                    List.of(singleA.orElseThrow().block().hardConstraint()),
                    List.of());
        }

        HardChoice a = singleA.orElseThrow();
        HardChoice b = singleB.orElseThrow();
        if (a.ability() != b.ability()) {
            return new HardSelection(
                    List.of(a.block().hardConstraint()),
                    List.of(b.block().hardConstraint()));
        }
        return a.block().delta() >= b.block().delta()
                ? new HardSelection(List.of(a.block().hardConstraint()), List.of())
                : new HardSelection(List.of(), List.of(b.block().hardConstraint()));
    }

    private List<AbilityWeight> hardAbilityWeights(
            WonderfulWolfIndividual parent) {
        if (!parent.phenotypeSnapshot().divineLineageExpressed()) {
            return List.of();
        }
        List<AbilityWeight> result = new ArrayList<>();
        for (Ability ability : Ability.values()) {
            double finalAbility =
                    parent.phenotypeSnapshot().abilities().get(ability);
            if (finalAbility > 1.0) {
                result.add(new AbilityWeight(
                        ability,
                        finalAbility - 1.0));
            }
        }
        return List.copyOf(result);
    }

    private Optional<HardChoice> hardSingle(
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
            List<AbilityWeight> original,
            GenomeRandom random) {
        List<AbilityWeight> remaining = new ArrayList<>(original);
        while (!remaining.isEmpty()) {
            AbilityWeight selected =
                    chooseWeighted(remaining, AbilityWeight::weight, random);
            Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> block =
                    bestExtraordinaryBlock(analysis, selected.ability());
            if (block.isPresent()) {
                return Optional.of(new HardChoice(
                        selected.ability(),
                        block.orElseThrow()));
            }
            remaining.remove(selected);
        }
        return Optional.empty();
    }

    private static Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock>
            bestExtraordinaryBlock(
                    WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
                    Ability ability) {
        return analysis.extraordinaryBlocks(ability).stream()
                .max(Comparator.comparingDouble(
                        WonderfulWolfInheritanceAnalyzer.InheritanceBlock::delta));
    }

    private SoftSelection selectSoft(
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisA,
            Optional<TraitStrength> strengthA,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisB,
            Optional<TraitStrength> strengthB,
            List<InheritanceConstraint> hardA,
            List<InheritanceConstraint> hardB,
            WwwConfig.BreedingPolicy policy,
            GenomeRandom random) {
        if (strengthA.isEmpty() && strengthB.isEmpty()) {
            return SoftSelection.empty();
        }

        List<AbilityWeight> weightsA =
                strengthA.isPresent() ? directAbilityWeights(analysisA) : List.of();
        List<AbilityWeight> weightsB =
                strengthB.isPresent() ? directAbilityWeights(analysisB) : List.of();

        if (weightsA.isEmpty()) {
            return softSingle(analysisB, weightsB, hardB, random)
                    .map(choice -> new SoftSelection(
                            List.of(),
                            List.of(choice.block().softConstraint(
                                    WonderfulWolfBreedingPolicy.directRetentionProbability(
                                            strengthB.orElseThrow(), policy),
                                    policy.directInheritance().crossoverWeightInsideBlock()))))
                    .orElseGet(SoftSelection::empty);
        }
        if (weightsB.isEmpty()) {
            return softSingle(analysisA, weightsA, hardA, random)
                    .map(choice -> new SoftSelection(
                            List.of(choice.block().softConstraint(
                                    WonderfulWolfBreedingPolicy.directRetentionProbability(
                                            strengthA.orElseThrow(), policy),
                                    policy.directInheritance().crossoverWeightInsideBlock())),
                            List.of()))
                    .orElseGet(SoftSelection::empty);
        }

        List<AbilityPair> pairs = distinctPairs(weightsA, weightsB);
        while (!pairs.isEmpty()) {
            AbilityPair selected =
                    chooseWeighted(pairs, AbilityPair::weight, random);
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocksA =
                    availableNormalBlocks(
                            analysisA, selected.a().ability(), hardA);
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocksB =
                    availableNormalBlocks(
                            analysisB, selected.b().ability(), hardB);

            if (!blocksA.isEmpty() && !blocksB.isEmpty()) {
                WonderfulWolfInheritanceAnalyzer.InheritanceBlock blockA =
                        chooseWeighted(
                                blocksA,
                                block -> block.delta() * block.delta(),
                                random);
                WonderfulWolfInheritanceAnalyzer.InheritanceBlock blockB =
                        chooseWeighted(
                                blocksB,
                                block -> block.delta() * block.delta(),
                                random);
                return new SoftSelection(
                        List.of(blockA.softConstraint(
                                WonderfulWolfBreedingPolicy.directRetentionProbability(
                                        strengthA.orElseThrow(), policy),
                                policy.directInheritance().crossoverWeightInsideBlock())),
                        List.of(blockB.softConstraint(
                                WonderfulWolfBreedingPolicy.directRetentionProbability(
                                        strengthB.orElseThrow(), policy),
                                policy.directInheritance().crossoverWeightInsideBlock())));
            }

            if (blocksA.isEmpty()) {
                pairs = pairs.stream()
                        .filter(pair -> pair.a().ability() != selected.a().ability())
                        .toList();
            }
            if (blocksB.isEmpty()) {
                pairs = pairs.stream()
                        .filter(pair -> pair.b().ability() != selected.b().ability())
                        .toList();
            }
        }

        Optional<SoftChoice> singleA =
                softSingle(analysisA, weightsA, hardA, random);
        Optional<SoftChoice> singleB =
                softSingle(analysisB, weightsB, hardB, random);
        if (singleA.isEmpty() && singleB.isEmpty()) {
            return SoftSelection.empty();
        }
        if (singleA.isEmpty()) {
            return new SoftSelection(
                    List.of(),
                    List.of(toSoftConstraint(
                            singleB.orElseThrow().block(),
                            strengthB.orElseThrow(),
                            policy)));
        }
        if (singleB.isEmpty()) {
            return new SoftSelection(
                    List.of(toSoftConstraint(
                            singleA.orElseThrow().block(),
                            strengthA.orElseThrow(),
                            policy)),
                    List.of());
        }

        SoftChoice a = singleA.orElseThrow();
        SoftChoice b = singleB.orElseThrow();
        if (a.ability() != b.ability()) {
            return new SoftSelection(
                    List.of(toSoftConstraint(
                            a.block(), strengthA.orElseThrow(), policy)),
                    List.of(toSoftConstraint(
                            b.block(), strengthB.orElseThrow(), policy)));
        }

        double wa = a.block().delta() * a.block().delta();
        double wb = b.block().delta() * b.block().delta();
        boolean chooseA = (wa + wb) > 0.0
                ? random.nextDouble() * (wa + wb) < wa
                : random.nextBoolean();
        return chooseA
                ? new SoftSelection(
                        List.of(toSoftConstraint(
                                a.block(), strengthA.orElseThrow(), policy)),
                        List.of())
                : new SoftSelection(
                        List.of(),
                        List.of(toSoftConstraint(
                                b.block(), strengthB.orElseThrow(), policy)));
    }

    private static List<AbilityWeight> directAbilityWeights(
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis) {
        List<AbilityWeight> result = new ArrayList<>();
        for (Ability ability : Ability.values()) {
            double base = analysis.baseAbility(ability);
            result.add(new AbilityWeight(ability, base * base));
        }
        return List.copyOf(result);
    }

    private Optional<SoftChoice> softSingle(
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
            List<AbilityWeight> original,
            List<InheritanceConstraint> hard,
            GenomeRandom random) {
        List<AbilityWeight> remaining = new ArrayList<>(original);
        while (!remaining.isEmpty()) {
            AbilityWeight selected =
                    chooseWeighted(remaining, AbilityWeight::weight, random);
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocks =
                    availableNormalBlocks(
                            analysis, selected.ability(), hard);
            if (!blocks.isEmpty()) {
                WonderfulWolfInheritanceAnalyzer.InheritanceBlock block =
                        chooseWeighted(
                                blocks,
                                candidate -> candidate.delta() * candidate.delta(),
                                random);
                return Optional.of(new SoftChoice(
                        selected.ability(),
                        block));
            }
            remaining.remove(selected);
        }
        return Optional.empty();
    }

    private static List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock>
            availableNormalBlocks(
                    WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
                    Ability ability,
                    List<InheritanceConstraint> hard) {
        return analysis.normalBlocks(ability).stream()
                .filter(block -> hard.stream().noneMatch(
                        constraint -> overlaps(constraint, block)))
                .toList();
    }

    private static InheritanceConstraint toSoftConstraint(
            WonderfulWolfInheritanceAnalyzer.InheritanceBlock block,
            TraitStrength strength,
            WwwConfig.BreedingPolicy policy) {
        return block.softConstraint(
                WonderfulWolfBreedingPolicy.directRetentionProbability(
                        strength, policy),
                policy.directInheritance().crossoverWeightInsideBlock());
    }

    private static List<AbilityPair> distinctPairs(
            List<AbilityWeight> a,
            List<AbilityWeight> b) {
        List<AbilityPair> result = new ArrayList<>();
        for (AbilityWeight left : a) {
            for (AbilityWeight right : b) {
                if (left.ability() != right.ability()) {
                    result.add(new AbilityPair(
                            left,
                            right,
                            left.weight() * right.weight()));
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean overlaps(
            InheritanceConstraint constraint,
            WonderfulWolfInheritanceAnalyzer.InheritanceBlock block) {
        return constraint.chromosomeIndex() == block.chromosomeIndex()
                && constraint.startBit() < block.endBitExclusive()
                && block.startBit() < constraint.endBitExclusive();
    }

    private static List<InheritanceConstraint> combine(
            List<InheritanceConstraint> hard,
            List<InheritanceConstraint> soft) {
        List<InheritanceConstraint> result =
                new ArrayList<>(hard.size() + soft.size());
        result.addAll(hard);
        result.addAll(soft);
        return List.copyOf(result);
    }

    private static <T> T chooseWeighted(
            List<T> values,
            ToDoubleFunction<T> weight,
            GenomeRandom random) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    "cannot choose from an empty list");
        }

        double total = 0.0;
        for (T value : values) {
            double candidate = weight.applyAsDouble(value);
            if (Double.isFinite(candidate) && candidate > 0.0) {
                total += candidate;
            }
        }
        if (!(total > 0.0) || !Double.isFinite(total)) {
            return values.get(random.nextInt(values.size()));
        }

        double draw = random.nextDouble() * total;
        double cumulative = 0.0;
        for (T value : values) {
            double candidate = weight.applyAsDouble(value);
            if (Double.isFinite(candidate) && candidate > 0.0) {
                cumulative += candidate;
                if (draw < cumulative) {
                    return value;
                }
            }
        }
        return values.getLast();
    }

    private record AbilityWeight(Ability ability, double weight) {
        private AbilityWeight {
            Objects.requireNonNull(ability, "ability");
        }
    }

    private record AbilityPair(
            AbilityWeight a,
            AbilityWeight b,
            double weight) {}

    private record HardChoice(
            Ability ability,
            WonderfulWolfInheritanceAnalyzer.InheritanceBlock block) {}

    private record HardSelection(
            List<InheritanceConstraint> parentA,
            List<InheritanceConstraint> parentB) {
        private HardSelection {
            parentA = List.copyOf(parentA);
            parentB = List.copyOf(parentB);
        }

        static HardSelection empty() {
            return new HardSelection(List.of(), List.of());
        }
    }

    private record SoftChoice(
            Ability ability,
            WonderfulWolfInheritanceAnalyzer.InheritanceBlock block) {}

    private record SoftSelection(
            List<InheritanceConstraint> parentA,
            List<InheritanceConstraint> parentB) {
        private SoftSelection {
            parentA = List.copyOf(parentA);
            parentB = List.copyOf(parentB);
        }

        static SoftSelection empty() {
            return new SoftSelection(List.of(), List.of());
        }
    }
}
