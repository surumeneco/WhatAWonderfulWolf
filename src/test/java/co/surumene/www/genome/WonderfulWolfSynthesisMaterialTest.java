package co.surumene.www.genome;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.DevelopmentFactor;
import co.surumene.www.domain.PersonalityFactor;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.founder.FounderRelationshipTarget;
import co.surumene.www.founder.FounderTarget;
import co.surumene.www.founder.WonderfulWolfSynthesisTarget;
import co.surumene.wgl.api.BitSequence;
import co.surumene.wgl.api.GeneSequenceCodec;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeRandom;
import co.surumene.wgl.api.SynthesisContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfSynthesisMaterialTest {
    private final WwwConfig config = WwwConfigLoader.loadDefaults();

    @Test
    void naturalFounderSuppliesLatentInjuryAndRegulationButNeverDivineOrExtraordinary() {
        RecordingCodec codec = new RecordingCodec();
        WonderfulWolfGenomeProfile profile = new WonderfulWolfGenomeProfile(config, codec);
        WonderfulWolfSynthesisTarget target = synthesisTarget(FounderOrigin.NATURAL, 0.40);

        profile.synthesisBlocks(target, SynthesisContext.defaults(), new FixedRandom(0.5));

        assertTrue(codec.addresses.stream().anyMatch(a -> a.type() == 0x02));
        assertTrue(codec.addresses.stream().anyMatch(a -> a.type() == 0x08));
        assertTrue(codec.addresses.stream().anyMatch(a -> a.type() == 0x08 && a.target() == 0x06));
        assertTrue(codec.relaySourceMagnitudes().stream().allMatch(magnitude -> magnitude == 1));
        assertFalse(codec.addresses.stream().anyMatch(a -> a.type() == 0x05));
        assertFalse(codec.addresses.stream().anyMatch(a -> a.type() == 0x07));
        assertFalse(target.continuousTargets().keySet().stream().anyMatch(a -> a.type() == 0x02));
    }

    @Test
    void wolfTrapAtOrBelowOneNeverSuppliesExtraordinaryButCanSupplyDivineIndependently() {
        RecordingCodec codec = new RecordingCodec();
        WonderfulWolfGenomeProfile profile = new WonderfulWolfGenomeProfile(config, codec);
        WonderfulWolfSynthesisTarget target = synthesisTarget(FounderOrigin.WOLF_TRAP, 0.80);

        profile.synthesisBlocks(target, SynthesisContext.defaults(), new FixedRandom(0.1));

        assertFalse(codec.addresses.stream().anyMatch(a -> a.type() == 0x07));
        assertTrue(codec.addresses.stream().anyMatch(a -> a.type() == 0x05 && a.target() == 0x00));
    }

    @Test
    void wolfTrapAboveOneSuppliesExtraordinaryGenesAsProfileMaterial() {
        RecordingCodec codec = new RecordingCodec();
        WonderfulWolfGenomeProfile profile = new WonderfulWolfGenomeProfile(config, codec);
        WonderfulWolfSynthesisTarget target = synthesisTarget(FounderOrigin.WOLF_TRAP, 1.20);

        profile.synthesisBlocks(target, SynthesisContext.defaults(), new FixedRandom(0.5));

        assertTrue(codec.addresses.stream().anyMatch(a -> a.type() == 0x07));
        assertFalse(target.continuousTargets().keySet().stream().anyMatch(a -> a.type() == 0x07));
    }

    private WonderfulWolfSynthesisTarget synthesisTarget(FounderOrigin origin, double abilityValue) {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, abilityValue);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) personality.put(factor, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) development.put(factor, 0.5);
        FounderTarget founder = new FounderTarget(
                origin,
                abilities,
                personality,
                List.of(),
                development,
                new FounderRelationshipTarget(0.5, 0.5));
        return WonderfulWolfSynthesisTarget.from(founder, config, new FixedRandom(0.5));
    }

    private static final class RecordingCodec implements GeneSequenceCodec {
        private final List<GenomeAddress> addresses = new ArrayList<>();
        private final List<Integer> magnitudes = new ArrayList<>();

        @Override
        public BitSequence encodeDirectGene(
                GenomeAddress address,
                boolean negative,
                int magnitudeCode,
                int expressionCode,
                BitSequence extension) {
            addresses.add(address);
            magnitudes.add(magnitudeCode);
            return BitSequence.fromBits("1");
        }

        @Override
        public BitSequence encodeRawGene(
                GenomeAddress address,
                int rawEffectByte,
                int expressionCode,
                BitSequence extension) {
            addresses.add(address);
            return BitSequence.fromBits("1");
        }

        @Override
        public BitSequence encodeAddressHeader(GenomeAddress address) {
            return BitSequence.fromBits("0".repeat(22));
        }

        private List<Integer> relaySourceMagnitudes() {
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i + 1 < addresses.size(); i++) {
                GenomeAddress next = addresses.get(i + 1);
                if (next.type() == 0x08 && next.target() == 0x06) {
                    out.add(magnitudes.get(i));
                }
            }
            return out;
        }
    }

    private static final class FixedRandom implements GenomeRandom {
        private final double value;

        private FixedRandom(double value) {
            this.value = value;
        }

        @Override public long nextLong() { return 0L; }
        @Override public double nextDouble() { return value; }
        @Override public int nextInt(int bound) { return Math.min(bound - 1, bound / 2); }
        @Override public boolean nextBoolean() { return false; }
    }
}
