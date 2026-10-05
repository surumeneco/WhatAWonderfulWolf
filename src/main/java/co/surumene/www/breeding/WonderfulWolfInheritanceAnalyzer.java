package co.surumene.www.breeding;

import co.surumene.www.domain.Ability;
import co.surumene.www.genome.WonderfulWolfDecodedPhenotype;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wgl.api.*;

import java.util.*;

final class WonderfulWolfInheritanceAnalyzer {
    private static final double EPS = 1.0e-12;

    private final GenomeEngine engine;
    private final WonderfulWolfGenomeProfile profile;

    WonderfulWolfInheritanceAnalyzer(
            GenomeEngine engine,
            WonderfulWolfGenomeProfile profile) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    ParentAnalysis analyze(WonderfulWolfIndividual individual) {
        Objects.requireNonNull(individual, "individual");
        DecodeResult<WonderfulWolfDecodedPhenotype> decoded =
                engine.decode(profile, individual.genome());
        return new ParentAnalysis(individual.genome(), decoded);
    }

    final class ParentAnalysis {
        private final DiploidGenome genome;
        private final DecodeResult<WonderfulWolfDecodedPhenotype> decoded;
        private final EnumMap<Ability, List<InheritanceBlock>> normal =
                new EnumMap<>(Ability.class);
        private final EnumMap<Ability, List<InheritanceBlock>> extraordinary =
                new EnumMap<>(Ability.class);

        private ParentAnalysis(
                DiploidGenome genome,
                DecodeResult<WonderfulWolfDecodedPhenotype> decoded) {
            this.genome = genome;
            this.decoded = decoded;
        }

        double baseAbility(Ability ability) {
            return decoded.phenotype().baseAbilities().get(ability);
        }

        List<InheritanceBlock> normalBlocks(Ability ability) {
            return normal.computeIfAbsent(
                    ability,
                    key -> blocksFor(key, 0x00, false));
        }

        List<InheritanceBlock> extraordinaryBlocks(Ability ability) {
            return extraordinary.computeIfAbsent(
                    ability,
                    key -> blocksFor(key, 0x07, true));
        }

        private List<InheritanceBlock> blocksFor(
                Ability ability,
                int addressType,
                boolean extraordinaryContribution) {
            GenomeAddress targetAddress =
                    new GenomeAddress(addressType, ability.targetId());

            List<DecodedGene> genes = decoded.decodedGenome().physicalGenes().stream()
                    .filter(DecodedGene::addressValid)
                    .filter(gene -> gene.chromosomeIndex() >= 0 && gene.haplotypeIndex() >= 0)
                    .sorted(Comparator
                            .comparingInt(DecodedGene::chromosomeIndex)
                            .thenComparingInt(DecodedGene::haplotypeIndex)
                            .thenComparingInt(DecodedGene::startBit))
                    .toList();

            List<BlockRange> candidates = new ArrayList<>();
            for (DecodedGene direct : genes) {
                if (!targetAddress.equals(direct.address())) {
                    continue;
                }
                candidates.add(expandLocalBlock(direct, genes));
            }

            List<BlockRange> merged = merge(candidates);
            double fullValue = extraordinaryContribution
                    ? decoded.phenotype().extraordinaryContributions().get(ability)
                    : decoded.phenotype().baseAbilities().get(ability);

            List<InheritanceBlock> result = new ArrayList<>();
            for (BlockRange range : merged) {
                DiploidGenome masked = mask(genome, range);
                WonderfulWolfDecodedPhenotype without =
                        engine.decode(profile, masked).phenotype();
                double maskedValue = extraordinaryContribution
                        ? without.extraordinaryContributions().get(ability)
                        : without.baseAbilities().get(ability);
                double delta = fullValue - maskedValue;
                if (delta > EPS) {
                    result.add(new InheritanceBlock(
                            ability,
                            range.chromosomeIndex(),
                            range.haplotypeIndex(),
                            range.startBit(),
                            range.endBitExclusive(),
                            delta));
                }
            }

            result.sort(Comparator
                    .comparingInt(InheritanceBlock::chromosomeIndex)
                    .thenComparingInt(InheritanceBlock::haplotypeIndex)
                    .thenComparingInt(InheritanceBlock::startBit));
            return List.copyOf(result);
        }
    }

    private static BlockRange expandLocalBlock(
            DecodedGene direct,
            List<DecodedGene> allGenes) {
        int start = direct.startBit();
        int end = direct.endBitExclusive();

        List<DecodedGene> lane = allGenes.stream()
                .filter(gene -> gene.chromosomeIndex() == direct.chromosomeIndex())
                .filter(gene -> gene.haplotypeIndex() == direct.haplotypeIndex())
                .toList();

        for (DecodedGene gene : lane) {
            if (!isCis(gene)) {
                continue;
            }
            int radius = 32 * (((gene.rawEffectByte() >>> 4) & 0x0F) + 1);
            if (Math.abs(gene.startBit() - direct.startBit()) <= radius) {
                start = Math.min(start, gene.startBit());
                end = Math.max(end, gene.endBitExclusive());
            }
        }

        int index = lane.indexOf(direct);
        if (index >= 0) {
            if (index + 1 < lane.size()) {
                DecodedGene after = lane.get(index + 1);
                if (isRelay(after)
                        && after.orientation() == GeneOrientation.FORWARD) {
                    start = Math.min(start, after.startBit());
                    end = Math.max(end, after.endBitExclusive());
                }
            }
            if (index > 0) {
                DecodedGene before = lane.get(index - 1);
                if (isRelay(before)
                        && before.orientation() == GeneOrientation.REVERSE) {
                    start = Math.min(start, before.startBit());
                    end = Math.max(end, before.endBitExclusive());
                }
            }
        }

        return new BlockRange(
                direct.chromosomeIndex(),
                direct.haplotypeIndex(),
                start,
                end);
    }

    private static boolean isRelay(DecodedGene gene) {
        return gene.addressValid()
                && gene.address() != null
                && gene.address().type() == 0x08
                && gene.address().target() == 0x06;
    }

    private static boolean isCis(DecodedGene gene) {
        return gene.addressValid()
                && gene.address() != null
                && gene.address().type() == 0x08
                && (gene.address().target() == 0x00 || gene.address().target() == 0x01);
    }

    private static List<BlockRange> merge(List<BlockRange> ranges) {
        if (ranges.isEmpty()) {
            return List.of();
        }
        List<BlockRange> sorted = ranges.stream()
                .sorted(Comparator
                        .comparingInt(BlockRange::chromosomeIndex)
                        .thenComparingInt(BlockRange::haplotypeIndex)
                        .thenComparingInt(BlockRange::startBit))
                .toList();

        List<BlockRange> merged = new ArrayList<>();
        BlockRange current = sorted.getFirst();
        for (int i = 1; i < sorted.size(); i++) {
            BlockRange next = sorted.get(i);
            if (current.chromosomeIndex() == next.chromosomeIndex()
                    && current.haplotypeIndex() == next.haplotypeIndex()
                    && next.startBit() <= current.endBitExclusive()) {
                current = new BlockRange(
                        current.chromosomeIndex(),
                        current.haplotypeIndex(),
                        current.startBit(),
                        Math.max(current.endBitExclusive(), next.endBitExclusive()));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return List.copyOf(merged);
    }

    private static DiploidGenome mask(
            DiploidGenome genome,
            BlockRange range) {
        List<ChromosomePair> pairs = new ArrayList<>(genome.chromosomePairs());
        ChromosomePair pair = pairs.get(range.chromosomeIndex());
        int length = range.endBitExclusive() - range.startBit();
        BitSequence replacement = BitSequence.ofPacked(
                new byte[BitSequence.packedLength(length)],
                length);

        BitSequence a = pair.haplotypeA();
        BitSequence b = pair.haplotypeB();
        if (range.haplotypeIndex() == 0) {
            a = a.replace(range.startBit(), range.endBitExclusive(), replacement);
        } else {
            b = b.replace(range.startBit(), range.endBitExclusive(), replacement);
        }
        pairs.set(range.chromosomeIndex(), new ChromosomePair(a, b));
        return new DiploidGenome(genome.genomeFormatVersion(), pairs);
    }

    record InheritanceBlock(
            Ability ability,
            int chromosomeIndex,
            int haplotypeIndex,
            int startBit,
            int endBitExclusive,
            double delta) {

        InheritanceBlock {
            Objects.requireNonNull(ability, "ability");
            if (chromosomeIndex < 0) throw new IllegalArgumentException("chromosomeIndex must be >= 0");
            if (haplotypeIndex < 0 || haplotypeIndex > 1) {
                throw new IllegalArgumentException("haplotypeIndex must be 0 or 1");
            }
            if (startBit < 0 || endBitExclusive <= startBit) {
                throw new IllegalArgumentException("invalid block range");
            }
            if (!Double.isFinite(delta) || delta <= 0.0) {
                throw new IllegalArgumentException("delta must be finite and > 0");
            }
        }

        InheritanceConstraint hardConstraint() {
            return InheritanceConstraint.hard(
                    chromosomeIndex, haplotypeIndex, startBit, endBitExclusive);
        }

        InheritanceConstraint softConstraint(
                double retentionProbability,
                double crossoverWeightMultiplier) {
            return InheritanceConstraint.soft(
                    chromosomeIndex,
                    haplotypeIndex,
                    startBit,
                    endBitExclusive,
                    retentionProbability,
                    crossoverWeightMultiplier);
        }
    }

    private record BlockRange(
            int chromosomeIndex,
            int haplotypeIndex,
            int startBit,
            int endBitExclusive) {}
}
