package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.InjuryPhenotype;
import co.surumene.wgl.api.AddressAggregate;
import co.surumene.wgl.api.BitSequence;
import co.surumene.wgl.api.DecodedGene;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.DecodedHomologyBlock;
import co.surumene.wgl.api.EffectiveContribution;
import co.surumene.wgl.api.GeneOrientation;
import co.surumene.wgl.api.GenomeAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfInjuryDecoderTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void profileRequestsHomologyContextForRecessiveInjuryDecoding() {
        assertTrue(profile.requiresHomologyContext());
    }

    @Test
    void oneSidedInjuryLoadRemainsCarrierWithoutPhenotype() {
        GenomeAddress injury = new GenomeAddress(0x02, Ability.HEALTH.targetId());
        DecodedGenome decoded = decoded(
                injury,
                List.of(contribution(injury, 0, 120, 0.60)),
                List.of(gene(injury, 0, 120, 128, 255)),
                List.of(new DecodedHomologyBlock(0, 100, 200, 100, 200)));

        assertTrue(profile.mapPhenotype(decoded).injuries().isEmpty());
    }

    @Test
    void homologousBilateralLoadAtThresholdProducesOneAbilityInjury() {
        GenomeAddress injury = new GenomeAddress(0x02, Ability.HEALTH.targetId());
        List<EffectiveContribution> contributions = List.of(
                contribution(injury, 0, 120, 0.60),
                contribution(injury, 1, 130, 0.60));
        List<DecodedGene> genes = List.of(
                gene(injury, 0, 120, 128, 255),
                gene(injury, 1, 130, 128, 255));
        DecodedGenome decoded = decoded(
                injury,
                contributions,
                genes,
                List.of(new DecodedHomologyBlock(0, 100, 200, 100, 200)));

        List<InjuryPhenotype> injuries = profile.mapPhenotype(decoded).injuries();

        assertEquals(1, injuries.size());
        InjuryPhenotype phenotype = injuries.getFirst();
        assertEquals(Ability.HEALTH, phenotype.ability());
        assertEquals(Math.round(7872.0 * 128.0 / 255.0), phenotype.onsetGameDay());
        assertEquals(6.0, phenotype.severityRank(), 1.0e-12);
    }


    @Test
    void multipleHomologyBlocksUsePairLoadAsOnsetAndSeverityWeight() {
        GenomeAddress injury = new GenomeAddress(0x02, Ability.HEALTH.targetId());
        List<EffectiveContribution> contributions = List.of(
                contribution(injury, 0, 120, 0.60),
                contribution(injury, 1, 130, 0.60),
                contribution(injury, 0, 320, 0.30),
                contribution(injury, 1, 330, 0.30));
        List<DecodedGene> genes = List.of(
                gene(injury, 0, 120, 0, 0),
                gene(injury, 1, 130, 0, 0),
                gene(injury, 0, 320, 255, 255),
                gene(injury, 1, 330, 255, 255));
        DecodedGenome decoded = decoded(
                injury,
                contributions,
                genes,
                List.of(
                        new DecodedHomologyBlock(0, 100, 200, 100, 200),
                        new DecodedHomologyBlock(0, 300, 400, 300, 400)));

        InjuryPhenotype phenotype = profile.mapPhenotype(decoded).injuries().getFirst();

        assertEquals(2624.0, phenotype.onsetGameDay(), 1.0e-12);
        assertEquals(4.0, phenotype.severityRank(), 1.0e-12);
    }

    private static DecodedGenome decoded(
            GenomeAddress address,
            List<EffectiveContribution> contributions,
            List<DecodedGene> genes,
            List<DecodedHomologyBlock> homologyBlocks) {
        return new DecodedGenome(
                Map.of(address, aggregate(contributions)),
                genes,
                homologyBlocks);
    }

    private static AddressAggregate aggregate(List<EffectiveContribution> contributions) {
        double positiveSurvival = 1.0;
        double negativeSurvival = 1.0;
        for (EffectiveContribution contribution : contributions) {
            if (contribution.effect() >= 0.0) {
                positiveSurvival *= 1.0 - contribution.saturation();
            } else {
                negativeSurvival *= 1.0 - contribution.saturation();
            }
        }
        double positive = 1.0 - positiveSurvival;
        return new AddressAggregate(
                positive,
                negativeSurvival,
                positive * negativeSurvival,
                contributions);
    }

    private static EffectiveContribution contribution(
            GenomeAddress address, int haplotype, int start, double saturation) {
        return new EffectiveContribution(address, 1.0, saturation, 0, haplotype, start, false);
    }

    private static DecodedGene gene(
            GenomeAddress address, int haplotype, int start, int onset, int severity) {
        return new DecodedGene(
                address,
                true,
                false,
                false,
                1,
                1,
                15,
                extension(onset, severity),
                0,
                haplotype,
                start,
                start + 82,
                GeneOrientation.FORWARD,
                0);
    }

    private static BitSequence extension(int onset, int severity) {
        return BitSequence.fromBits(bits8(onset) + bits8(severity));
    }

    private static String bits8(int value) {
        return String.format("%8s", Integer.toBinaryString(value & 0xFF)).replace(' ', '0');
    }
}
