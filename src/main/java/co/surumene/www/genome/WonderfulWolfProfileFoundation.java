package co.surumene.www.genome;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.wgl.api.AnchorSeed;
import co.surumene.wgl.api.BackboneDefinition;
import co.surumene.wgl.api.BitSequence;
import co.surumene.wgl.api.ChromosomeTemplate;
import co.surumene.wgl.api.FounderScaffoldTolerance;
import co.surumene.wgl.api.MarkerLocus;
import co.surumene.wgl.api.ProfileDescriptor;
import co.surumene.wgl.api.StandardGenomeSafetyPolicyV1;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class WonderfulWolfProfileFoundation {
    public static final String PROFILE_ID = "wonderful-wolf";
    public static final int PROFILE_VERSION = 1;
    public static final int GENOME_FORMAT_VERSION = 1;

    private static final List<Integer> BASELINE_LENGTHS =
            List.of(9216, 8192, 7168, 6144, 5120, 4096);
    private static final int ANCHOR_INTERVAL = 512;
    private static final int ANCHOR_START = 256;
    private static final long TEMPLATE_SEED = 0x5757575F4241434BL;

    private WonderfulWolfProfileFoundation() {}

    public static ProfileDescriptor descriptor(WwwConfig config) {
        WwwConfig validated = WwwConfigValidator.validate(config);
        return new ProfileDescriptor(
                PROFILE_ID,
                PROFILE_VERSION,
                semanticFingerprint(validated.genomeProfile()));
    }

    public static BackboneDefinition backbone(WwwConfig config) {
        WwwConfig validated = WwwConfigValidator.validate(config);
        WwwConfig.Synthesizer synthesizer = validated.genomeProfile().synthesizer();
        FounderScaffoldTolerance tolerance = new FounderScaffoldTolerance(
                synthesizer.chromosomeLengthStandardDeviationRatio(),
                synthesizer.chromosomeLengthMinRatio(),
                synthesizer.chromosomeLengthMaxRatio());

        List<ChromosomeTemplate> chromosomes = new ArrayList<>(BASELINE_LENGTHS.size());
        for (int index = 0; index < BASELINE_LENGTHS.size(); index++) {
            int bitLength = BASELINE_LENGTHS.get(index);
            BitSequence template = template(index, bitLength);
            List<AnchorSeed> anchors = anchors(template);
            MarkerLocus markerLocus = markerLocus(anchors, bitLength);
            chromosomes.add(new ChromosomeTemplate(template, anchors, markerLocus, tolerance));
        }

        return new BackboneDefinition(
                PROFILE_ID,
                GENOME_FORMAT_VERSION,
                chromosomes,
                new StandardGenomeSafetyPolicyV1());
    }

    private static byte[] semanticFingerprint(WwwConfig.GenomeProfile profile) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            CanonicalDigest out = new CanonicalDigest(digest);

            WwwConfig.Decoder decoder = profile.decoder();
            out.doubles(
                    decoder.personality().seriousMaxScore(),
                    decoder.personality().seriousSpread(),
                    decoder.personality().dominantGap(),
                    decoder.trait().expressionThreshold(),
                    decoder.trait().strongGap(),
                    decoder.injury().expressionThreshold(),
                    decoder.injury().onsetMaxGameDays(),
                    decoder.injury().severityRankMin(),
                    decoder.injury().severityRankMax(),
                    decoder.divine().minHaplotypeScore(),
                    decoder.divine().totalScore());

            WwwConfig.Synthesizer synth = profile.synthesizer();
            out.doubles(
                    synth.injury().blocksLambda(),
                    synth.injury().loadMean(),
                    synth.injury().loadStandardDeviation(),
                    synth.injury().loadMin(),
                    synth.injury().loadMax(),
                    synth.injury().bilateralProbability(),
                    synth.injury().onsetMean(),
                    synth.injury().onsetStandardDeviation(),
                    synth.injury().severityMean(),
                    synth.injury().severityStandardDeviation());
            out.ints(synth.injury().blocksMax(), synth.injury().genesPerBlock());

            out.doubles(
                    synth.divine().supplyProbability(),
                    synth.divine().majorHaplotypeProbability(),
                    synth.divine().geneDMin(),
                    synth.divine().geneDMax());
            out.ints(
                    synth.divine().genesMin(),
                    synth.divine().genesCenter(),
                    synth.divine().genesMax());

            out.doubleValue(synth.extraordinary().transferMax());
            out.ints(
                    synth.extraordinary().genesPerTargetMin(),
                    synth.extraordinary().genesPerTargetMax(),
                    synth.extraordinary().blocksPerTargetMin(),
                    synth.extraordinary().blocksPerTargetMax());

            out.doubles(
                    synth.cancellationMin(),
                    synth.cancellationMax(),
                    synth.highTargetHeadroom(),
                    synth.personalityCancellationMin(),
                    synth.personalityCancellationMax(),
                    synth.chromosomeLengthStandardDeviationRatio(),
                    synth.chromosomeLengthMinRatio(),
                    synth.chromosomeLengthMaxRatio(),
                    synth.recognizableRegionMaxRatio(),
                    synth.noncodingRegionMinRatio());

            digestRange(out, synth.genesPerTarget().ability());
            digestRange(out, synth.genesPerTarget().personality());
            digestRange(out, synth.genesPerTarget().development());
            digestRange(out, synth.genesPerTarget().relationship());
            digestRange(out, synth.genesPerTarget().trait());

            out.ints(
                    synth.directGenesSoftMin(),
                    synth.directGenesSoftMax(),
                    synth.directGenesHardMax(),
                    synth.regulationGenesMin(),
                    synth.regulationGenesCenter(),
                    synth.regulationGenesMax(),
                    synth.regulationGenesHardMax(),
                    synth.recognizableGenesHardMax());

            out.doubles(
                    synth.relay().attachmentMinRatio(),
                    synth.relay().attachmentMaxRatio(),
                    synth.relay().normalSecondaryMinRatio(),
                    synth.relay().normalSecondaryMaxRatio(),
                    synth.relay().strongSecondaryMinRatio(),
                    synth.relay().strongSecondaryMaxRatio(),
                    synth.relay().positiveRatio());

            WwwConfig.BreedingPolicy breeding = profile.breedingPolicy();
            out.doubles(
                    breeding.directInheritance().weakPreferProbability(),
                    breeding.directInheritance().crossoverWeightInsideBlock(),
                    breeding.wildTrait().weakParentMultiplier(),
                    breeding.wildTrait().strongParentMultiplier());

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void digestRange(CanonicalDigest out, WwwConfig.Range range) {
        out.ints(range.min(), range.center(), range.max());
    }

    private static BitSequence template(int chromosomeIndex, int bitLength) {
        byte[] packed = new byte[BitSequence.packedLength(bitLength)];
        long state = TEMPLATE_SEED ^ ((long) chromosomeIndex * 0x9E3779B97F4A7C15L);
        for (int offset = 0; offset < packed.length; offset += Long.BYTES) {
            state += 0x9E3779B97F4A7C15L;
            long value = mix64(state);
            for (int byteIndex = 0; byteIndex < Long.BYTES && offset + byteIndex < packed.length; byteIndex++) {
                packed[offset + byteIndex] =
                        (byte) (value >>> ((Long.BYTES - 1 - byteIndex) * Byte.SIZE));
            }
        }
        return BitSequence.ofPacked(packed, bitLength);
    }

    private static List<AnchorSeed> anchors(BitSequence template) {
        List<AnchorSeed> anchors = new ArrayList<>();
        for (int position = ANCHOR_START;
             position + 48 <= template.bitLength();
             position += ANCHOR_INTERVAL) {
            anchors.add(new AnchorSeed(position, template.slice(position, position + 48)));
        }
        if (anchors.size() < 2) {
            throw new IllegalStateException("backbone chromosome requires at least two anchors");
        }
        return List.copyOf(anchors);
    }

    private static MarkerLocus markerLocus(List<AnchorSeed> anchors, int chromosomeLength) {
        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        double chromosomeCenter = chromosomeLength / 2.0;
        for (int index = 0; index < anchors.size() - 1; index++) {
            double firstCenter = anchors.get(index).position() + 24.0;
            double secondCenter = anchors.get(index + 1).position() + 24.0;
            double pairCenter = (firstCenter + secondCenter) / 2.0;
            double distance = Math.abs(pairCenter - chromosomeCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return new MarkerLocus(anchors.get(bestIndex), anchors.get(bestIndex + 1));
    }

    private static long mix64(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static final class CanonicalDigest {
        private final MessageDigest digest;
        private final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);
        private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);

        private CanonicalDigest(MessageDigest digest) {
            this.digest = digest;
        }

        void doubles(double... values) {
            for (double value : values) {
                doubleValue(value);
            }
        }

        void doubleValue(double value) {
            longBuffer.clear();
            longBuffer.putLong(Double.doubleToLongBits(value));
            digest.update(longBuffer.array());
        }

        void ints(int... values) {
            for (int value : values) {
                intBuffer.clear();
                intBuffer.putInt(value);
                digest.update(intBuffer.array());
            }
        }
    }
}
