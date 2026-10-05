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
    private final GenomeEngine engine;
    private final WonderfulWolfGenomeProfile profile;
    private final WonderfulWolfInheritanceAnalyzer analyzer;

    public WonderfulWolfBreedingContextFactory(
            GenomeEngine engine,
            WonderfulWolfGenomeProfile profile) {
        this.engine = Objects.requireNonNull(engine, "engine");
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

        List<InheritanceConstraint> softA = List.of();
        List<InheritanceConstraint> softB = List.of();
        Optional<TraitStrength> directA =
                WonderfulWolfBreedingPolicy.expressedTrait(
                        parentA.phenotypeSnapshot(), Trait.DIRECT_INHERITANCE);
        Optional<TraitStrength> directB =
                WonderfulWolfBreedingPolicy.expressedTrait(
                        parentB.phenotypeSnapshot(), Trait.DIRECT_INHERITANCE);

        if (directA.isPresent() || directB.isPresent()) {
            SoftSelection soft = selectSoft(
                    parentA, analysisA, directA,
                    parentB, analysisB, directB,
                    hard.parentA(), hard.parentB(),
                    policy, random);
            softA = soft.parentA();
            softB = soft.parentB();
        }

        List<InheritanceConstraint> finalA =
                combine(hard.parentA(),
                        WonderfulWolfBreedingPolicy.resolveSoftAgainstHard(
                                hard.parentA(), softA));
        List<InheritanceConstraint> finalB =
                combine(hard.parentB(),
                        WonderfulWolfBreedingPolicy.resolveSoftAgainstHard(
                                hard.parentB(), softB));

        double mutationMultiplier =
                WonderfulWolfBreedingPolicy.mutationMultiplier(
                        parentA.phenotypeSnapshot(),
                        parentB.phenotypeSnapshot(),
                        policy);

        return new BreedingContext(
                profile.backbone(),
                mutationMultiplier,
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
        List<HardOption> optionsA = hardOptions(parentA, analysisA);
        List<HardOption> optionsB = hardOptions(parentB, analysisB);

        if (optionsA.isEmpty() && optionsB.isEmpty()) {
            return HardSelection.empty();
        }
        if (optionsA.isEmpty()) {
            HardOption selected = chooseWeighted(optionsB, HardOption::weight, random);
            return new HardSelection(List.of(), List.of(selected.block().hardConstraint()));
        }
        if (optionsB.isEmpty()) {
            HardOption selected = chooseWeighted(optionsA, HardOption::weight, random);
            return new HardSelection(List.of(selected.block().hardConstraint()), List.of());
        }

        List<HardPair> pairs = new ArrayList<>();
        for (HardOption a : optionsA) {
            for (HardOption b : optionsB) {
                if (a.ability() != b.ability()) {
                    pairs.add(new HardPair(a, b, a.weight() * b.weight()));
                }
            }
        }
        if (!pairs.isEmpty()) {
            HardPair selected = chooseWeighted(pairs, HardPair::weight, random);
            return new HardSelection(
                    List.of(selected.a().block().hardConstraint()),
                    List.of(selected.b().block().hardConstraint()));
        }

        HardOption bestA = optionsA.stream()
                .max(Comparator.comparingDouble(option -> option.block().delta()))
                .orElseThrow();
        HardOption bestB = optionsB.stream()
                .max(Comparator.comparingDouble(option -> option.block().delta()))
                .orElseThrow();
        if (bestA.block().delta() >= bestB.block().delta()) {
            return new HardSelection(List.of(bestA.block().hardConstraint()), List.of());
        }
        return new HardSelection(List.of(), List.of(bestB.block().hardConstraint()));
    }

    private List<HardOption> hardOptions(
            WonderfulWolfIndividual parent,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis) {
        if (!parent.phenotypeSnapshot().divineLineageExpressed()) {
            return List.of();
        }

        List<HardOption> result = new ArrayList<>();
        for (Ability ability : Ability.values()) {
            double finalAbility = parent.phenotypeSnapshot().abilities().get(ability);
            if (finalAbility <= 1.0) {
                continue;
            }
            Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> best =
                    analysis.extraordinaryBlocks(ability).stream()
                            .max(Comparator.comparingDouble(
                                    WonderfulWolfInheritanceAnalyzer.InheritanceBlock::delta));
            best.ifPresent(block ->
                    result.add(new HardOption(
                            ability,
                            finalAbility - 1.0,
                            block)));
        }
        return List.copyOf(result);
    }

    private SoftSelection selectSoft(
            WonderfulWolfIndividual parentA,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisA,
            Optional<TraitStrength> strengthA,
            WonderfulWolfIndividual parentB,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisB,
            Optional<TraitStrength> strengthB,
            List<InheritanceConstraint> hardA,
            List<InheritanceConstraint> hardB,
            WwwConfig.BreedingPolicy policy,
            GenomeRandom random) {
        List<SoftOption> optionsA = strengthA.isPresent()
                ? softOptions(analysisA, hardA)
                : List.of();
        List<SoftOption> optionsB = strengthB.isPresent()
                ? softOptions(analysisB, hardB)
                : List.of();

        if (optionsA.isEmpty() && optionsB.isEmpty()) {
            return SoftSelection.empty();
        }
        if (optionsA.isEmpty()) {
            return new SoftSelection(
                    List.of(),
                    List.of(toSoftConstraint(
                            chooseSoftOption(optionsB, random),
                            strengthB.orElseThrow(),
                            policy,
                            random)));
        }
        if (optionsB.isEmpty()) {
            return new SoftSelection(
                    List.of(toSoftConstraint(
                            chooseSoftOption(optionsA, random),
                            strengthA.orElseThrow(),
                            policy,
                            random)),
                    List.of());
        }

        List<SoftPair> pairs = new ArrayList<>();
        for (SoftOption a : optionsA) {
            for (SoftOption b : optionsB) {
                if (a.ability() != b.ability()) {
                    pairs.add(new SoftPair(a, b, a.weight() * b.weight()));
                }
            }
        }
        if (!pairs.isEmpty()) {
            SoftPair selected = chooseWeighted(pairs, SoftPair::weight, random);
            return new SoftSelection(
                    List.of(toSoftConstraint(
                            selected.a(), strengthA.orElseThrow(), policy, random)),
                    List.of(toSoftConstraint(
                            selected.b(), strengthB.orElseThrow(), policy, random)));
        }

        SoftOption selectedA = chooseSoftOption(optionsA, random);
        SoftOption selectedB = chooseSoftOption(optionsB, random);
        double blockWeightA = maxDeltaSquared(selectedA.blocks());
        double blockWeightB = maxDeltaSquared(selectedB.blocks());
        boolean chooseA;
        double total = blockWeightA + blockWeightB;
        if (total > 0.0 && Double.isFinite(total)) {
            chooseA = random.nextDouble() * total < blockWeightA;
        } else {
            chooseA = random.nextBoolean();
        }

        return chooseA
                ? new SoftSelection(
                        List.of(toSoftConstraint(
                                selectedA, strengthA.orElseThrow(), policy, random)),
                        List.of())
                : new SoftSelection(
                        List.of(),
                        List.of(toSoftConstraint(
                                selectedB, strengthB.orElseThrow(), policy, random)));
    }

    private List<SoftOption> softOptions(
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
            List<InheritanceConstraint> hard) {
        List<SoftOption> result = new ArrayList<>();
        for (Ability ability : Ability.values()) {
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocks =
                    analysis.normalBlocks(ability).stream()
                            .filter(block -> hard.stream().noneMatch(
                                    constraint -> overlaps(constraint, block)))
                            .toList();
            if (!blocks.isEmpty()) {
                double base = analysis.baseAbility(ability);
                result.add(new SoftOption(ability, base * base, blocks));
            }
        }
        return List.copyOf(result);
    }

    private InheritanceConstraint toSoftConstraint(
            SoftOption option,
            TraitStrength strength,
            WwwConfig.BreedingPolicy policy,
            GenomeRandom random) {
        WonderfulWolfInheritanceAnalyzer.InheritanceBlock block =
                chooseWeighted(
                        option.blocks(),
                        candidate -> candidate.delta() * candidate.delta(),
                        random);
        return block.softConstraint(
                WonderfulWolfBreedingPolicy.directRetentionProbability(strength, policy),
                policy.directInheritance().crossoverWeightInsideBlock());
    }

    private static SoftOption chooseSoftOption(
            List<SoftOption> options,
            GenomeRandom random) {
        return chooseWeighted(options, SoftOption::weight, random);
    }

    private static double maxDeltaSquared(
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocks) {
        return blocks.stream()
                .mapToDouble(block -> block.delta() * block.delta())
                .max()
                .orElse(0.0);
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
            throw new IllegalArgumentException("cannot choose from an empty list");
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

    private record HardOption(
            Ability ability,
            double weight,
            WonderfulWolfInheritanceAnalyzer.InheritanceBlock block) {}

    private record HardPair(HardOption a, HardOption b, double weight) {}

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

    private record SoftOption(
            Ability ability,
            double weight,
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocks) {
        private SoftOption {
            blocks = List.copyOf(blocks);
        }
    }

    private record SoftPair(SoftOption a, SoftOption b, double weight) {}

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
