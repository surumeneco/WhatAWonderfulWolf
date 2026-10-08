package co.surumene.www.breeding;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.*;
import co.surumene.www.genome.PhenotypeOrigin;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.*;
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
            BreedingParentSource parentA,
            BreedingParentSource parentB,
            GenomeRandom random) {
        Objects.requireNonNull(parentA, "parentA");
        Objects.requireNonNull(parentB, "parentB");
        Objects.requireNonNull(random, "random");
        return create(
                contextIndividual(parentA),
                contextIndividual(parentB),
                random);
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
        Optional<TraitStrength> directA =
                WonderfulWolfBreedingPolicy.expressedTrait(
                        parentA.phenotypeSnapshot(), Trait.DIRECT_INHERITANCE);
        Optional<TraitStrength> directB =
                WonderfulWolfBreedingPolicy.expressedTrait(
                        parentB.phenotypeSnapshot(), Trait.DIRECT_INHERITANCE);

        WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisA =
                directA.isPresent() || hasHardCandidate(parentA)
                        ? analyzer.analyze(parentA)
                        : null;
        WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisB =
                directB.isPresent() || hasHardCandidate(parentB)
                        ? analyzer.analyze(parentB)
                        : null;

        HardSelection hard = selectHard(
                parentA, analysisA,
                parentB, analysisB,
                random);

        SoftSelection soft = selectSoft(
                analysisA, directA, hard.parentA(),
                analysisB, directB, hard.parentB(),
                policy, random);

        List<InheritanceConstraint> finalA =
                combine(hard.parentA(), soft.parentA());
        List<InheritanceConstraint> finalB =
                combine(hard.parentB(), soft.parentB());

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

    private WonderfulWolfIndividual contextIndividual(
            BreedingParentSource source) {
        if (source instanceof BreedingParentSource.DiploidParent diploid) {
            return decodedIndividual(
                    diploid.genome(),
                    true);
        }

        HaploidGenome haploid =
                ((BreedingParentSource.Gamete) source).genome();
        List<ChromosomePair> pairs = haploid.chromosomes().stream()
                .map(bits -> new ChromosomePair(bits, bits))
                .toList();
        return decodedIndividual(
                new DiploidGenome(
                        haploid.genomeFormatVersion(),
                        pairs),
                false);
    }

    private WonderfulWolfIndividual decodedIndividual(
            DiploidGenome genome,
            boolean applyParentModifiers) {
        var decoded = engine.decode(profile, genome);
        PhenotypeSnapshot snapshot = decoded.phenotype().toSnapshot(
                decoded.identity(),
                PhenotypeOrigin.BREEDING);
        if (!applyParentModifiers) {
            snapshot = new PhenotypeSnapshot(
                    snapshot.decoderIdentity(),
                    snapshot.abilities(),
                    snapshot.relationshipPerformance(),
                    snapshot.personalityFactors(),
                    snapshot.personality(),
                    List.of(),
                    snapshot.developmentFactors(),
                    snapshot.injuries(),
                    0.0,
                    false);
        }
        return new WonderfulWolfIndividual(
                genome,
                snapshot,
                Optional.empty(),
                0L,
                Mode.WANDER,
                Optional.empty(),
                ActionDistance.NORMAL,
                Optional.empty(),
                Map.of(),
                Optional.empty(),
                Map.of(),
                0,
                PedigreeSnapshot.founder());
    }

    private static boolean hasHardCandidate(
            WonderfulWolfIndividual parent) {
        return parent.phenotypeSnapshot().divineLineageExpressed()
                && parent.phenotypeSnapshot().abilities().values().stream()
                        .anyMatch(value -> value > 1.0);
    }

    private HardSelection selectHard(
            WonderfulWolfIndividual parentA,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisA,
            WonderfulWolfIndividual parentB,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisB,
            GenomeRandom random) {
        List<AbilityCandidate> candidatesA =
                new ArrayList<>(hardCandidates(parentA));
        List<AbilityCandidate> candidatesB =
                new ArrayList<>(hardCandidates(parentB));

        while (!candidatesA.isEmpty() || !candidatesB.isEmpty()) {
            if (candidatesA.isEmpty()) {
                Optional<InheritanceConstraint> chosen =
                        chooseSingleHard(candidatesB, analysisB, random);
                return new HardSelection(List.of(), chosen.stream().toList());
            }
            if (candidatesB.isEmpty()) {
                Optional<InheritanceConstraint> chosen =
                        chooseSingleHard(candidatesA, analysisA, random);
                return new HardSelection(chosen.stream().toList(), List.of());
            }

            List<AbilityPair> pairs = distinctPairs(candidatesA, candidatesB);
            if (!pairs.isEmpty()) {
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
                    removeAbility(candidatesA, selected.a().ability());
                }
                if (blockB.isEmpty()) {
                    removeAbility(candidatesB, selected.b().ability());
                }
                continue;
            }

            AbilityCandidate onlyA = candidatesA.getFirst();
            AbilityCandidate onlyB = candidatesB.getFirst();
            Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blockA =
                    bestExtraordinaryBlock(analysisA, onlyA.ability());
            Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blockB =
                    bestExtraordinaryBlock(analysisB, onlyB.ability());

            if (blockA.isPresent() && blockB.isPresent()) {
                if (blockA.orElseThrow().delta() >= blockB.orElseThrow().delta()) {
                    return new HardSelection(
                            List.of(blockA.orElseThrow().hardConstraint()),
                            List.of());
                }
                return new HardSelection(
                        List.of(),
                        List.of(blockB.orElseThrow().hardConstraint()));
            }
            if (blockA.isEmpty()) {
                removeAbility(candidatesA, onlyA.ability());
            }
            if (blockB.isEmpty()) {
                removeAbility(candidatesB, onlyB.ability());
            }
        }

        return HardSelection.empty();
    }

    private Optional<InheritanceConstraint> chooseSingleHard(
            List<AbilityCandidate> candidates,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
            GenomeRandom random) {
        while (!candidates.isEmpty()) {
            AbilityCandidate selected =
                    chooseWeighted(candidates, AbilityCandidate::weight, random);
            Optional<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> block =
                    bestExtraordinaryBlock(analysis, selected.ability());
            if (block.isPresent()) {
                return Optional.of(block.orElseThrow().hardConstraint());
            }
            removeAbility(candidates, selected.ability());
        }
        return Optional.empty();
    }

    private static List<AbilityCandidate> hardCandidates(
            WonderfulWolfIndividual parent) {
        if (!parent.phenotypeSnapshot().divineLineageExpressed()) {
            return List.of();
        }
        List<AbilityCandidate> result = new ArrayList<>();
        for (Ability ability : Ability.values()) {
            double finalAbility =
                    parent.phenotypeSnapshot().abilities().get(ability);
            if (finalAbility > 1.0) {
                result.add(new AbilityCandidate(
                        ability,
                        finalAbility - 1.0));
            }
        }
        return List.copyOf(result);
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
            List<InheritanceConstraint> hardA,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysisB,
            Optional<TraitStrength> strengthB,
            List<InheritanceConstraint> hardB,
            WwwConfig.BreedingPolicy policy,
            GenomeRandom random) {
        List<AbilityCandidate> candidatesA = strengthA.isPresent()
                ? new ArrayList<>(normalCandidates(analysisA))
                : new ArrayList<>();
        List<AbilityCandidate> candidatesB = strengthB.isPresent()
                ? new ArrayList<>(normalCandidates(analysisB))
                : new ArrayList<>();

        while (!candidatesA.isEmpty() || !candidatesB.isEmpty()) {
            if (candidatesA.isEmpty()) {
                Optional<InheritanceConstraint> chosen =
                        chooseSingleSoft(
                                candidatesB, analysisB, hardB,
                                strengthB.orElseThrow(), policy, random);
                return new SoftSelection(List.of(), chosen.stream().toList());
            }
            if (candidatesB.isEmpty()) {
                Optional<InheritanceConstraint> chosen =
                        chooseSingleSoft(
                                candidatesA, analysisA, hardA,
                                strengthA.orElseThrow(), policy, random);
                return new SoftSelection(chosen.stream().toList(), List.of());
            }

            List<AbilityPair> pairs = distinctPairs(candidatesA, candidatesB);
            if (!pairs.isEmpty()) {
                AbilityPair selected =
                        chooseWeighted(pairs, AbilityPair::weight, random);
                List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocksA =
                        eligibleNormalBlocks(
                                analysisA, selected.a().ability(), hardA);
                List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocksB =
                        eligibleNormalBlocks(
                                analysisB, selected.b().ability(), hardB);

                if (!blocksA.isEmpty() && !blocksB.isEmpty()) {
                    return new SoftSelection(
                            List.of(toSoftConstraint(
                                    blocksA,
                                    strengthA.orElseThrow(),
                                    policy,
                                    random)),
                            List.of(toSoftConstraint(
                                    blocksB,
                                    strengthB.orElseThrow(),
                                    policy,
                                    random)));
                }
                if (blocksA.isEmpty()) {
                    removeAbility(candidatesA, selected.a().ability());
                }
                if (blocksB.isEmpty()) {
                    removeAbility(candidatesB, selected.b().ability());
                }
                continue;
            }

            AbilityCandidate onlyA = candidatesA.getFirst();
            AbilityCandidate onlyB = candidatesB.getFirst();
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocksA =
                    eligibleNormalBlocks(
                            analysisA, onlyA.ability(), hardA);
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocksB =
                    eligibleNormalBlocks(
                            analysisB, onlyB.ability(), hardB);

            if (!blocksA.isEmpty() && !blocksB.isEmpty()) {
                AbilityCandidate selectedParent = chooseWeighted(
                        List.of(onlyA, onlyB),
                        AbilityCandidate::weight,
                        random);
                if (selectedParent == onlyA) {
                    return new SoftSelection(
                            List.of(toSoftConstraint(
                                    blocksA,
                                    strengthA.orElseThrow(),
                                    policy,
                                    random)),
                            List.of());
                }
                return new SoftSelection(
                        List.of(),
                        List.of(toSoftConstraint(
                                blocksB,
                                strengthB.orElseThrow(),
                                policy,
                                random)));
            }
            if (blocksA.isEmpty()) {
                removeAbility(candidatesA, onlyA.ability());
            }
            if (blocksB.isEmpty()) {
                removeAbility(candidatesB, onlyB.ability());
            }
        }

        return SoftSelection.empty();
    }

    private Optional<InheritanceConstraint> chooseSingleSoft(
            List<AbilityCandidate> candidates,
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
            List<InheritanceConstraint> hard,
            TraitStrength strength,
            WwwConfig.BreedingPolicy policy,
            GenomeRandom random) {
        while (!candidates.isEmpty()) {
            AbilityCandidate selected =
                    chooseWeighted(candidates, AbilityCandidate::weight, random);
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocks =
                    eligibleNormalBlocks(
                            analysis, selected.ability(), hard);
            if (!blocks.isEmpty()) {
                return Optional.of(toSoftConstraint(
                        blocks, strength, policy, random));
            }
            removeAbility(candidates, selected.ability());
        }
        return Optional.empty();
    }

    private static List<AbilityCandidate> normalCandidates(
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis) {
        List<AbilityCandidate> result = new ArrayList<>();
        boolean anyPositive = false;
        for (Ability ability : Ability.values()) {
            double base = analysis.baseAbility(ability);
            double weight = base * base;
            result.add(new AbilityCandidate(ability, weight));
            anyPositive |= weight > 0.0;
        }
        if (!anyPositive) {
            return List.copyOf(result);
        }
        return result.stream()
                .filter(candidate -> candidate.weight() > 0.0)
                .toList();
    }

    private static List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock>
    eligibleNormalBlocks(
            WonderfulWolfInheritanceAnalyzer.ParentAnalysis analysis,
            Ability ability,
            List<InheritanceConstraint> hard) {
        return analysis.normalBlocks(ability).stream()
                .filter(block -> hard.stream().noneMatch(
                        constraint -> overlaps(constraint, block)))
                .toList();
    }

    private static InheritanceConstraint toSoftConstraint(
            List<WonderfulWolfInheritanceAnalyzer.InheritanceBlock> blocks,
            TraitStrength strength,
            WwwConfig.BreedingPolicy policy,
            GenomeRandom random) {
        WonderfulWolfInheritanceAnalyzer.InheritanceBlock block =
                chooseWeighted(
                        blocks,
                        candidate -> candidate.delta() * candidate.delta(),
                        random);
        return block.softConstraint(
                WonderfulWolfBreedingPolicy.directRetentionProbability(
                        strength, policy),
                policy.directInheritance().crossoverWeightInsideBlock());
    }

    private static List<AbilityPair> distinctPairs(
            List<AbilityCandidate> a,
            List<AbilityCandidate> b) {
        List<AbilityPair> result = new ArrayList<>();
        for (AbilityCandidate left : a) {
            for (AbilityCandidate right : b) {
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

    private static void removeAbility(
            List<AbilityCandidate> candidates,
            Ability ability) {
        candidates.removeIf(candidate -> candidate.ability() == ability);
    }

    private static List<InheritanceConstraint> combine(
            List<InheritanceConstraint> hard,
            List<InheritanceConstraint> soft) {
        List<InheritanceConstraint> result =
                new ArrayList<>(hard.size() + soft.size());
        result.addAll(hard);
        result.addAll(
                WonderfulWolfBreedingPolicy.resolveSoftAgainstHard(
                        hard, soft));
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

    private record AbilityCandidate(
            Ability ability,
            double weight) {}

    private record AbilityPair(
            AbilityCandidate a,
            AbilityCandidate b,
            double weight) {}

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
