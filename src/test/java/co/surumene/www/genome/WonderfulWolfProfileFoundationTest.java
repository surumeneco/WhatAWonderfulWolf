package co.surumene.www.genome;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.wgl.api.BackboneDefinition;
import co.surumene.wgl.api.ProfileDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfProfileFoundationTest {
    @Test
    void profileDescriptorUsesStableIdentityAndTypedProfileConfigurationFingerprint() {
        WwwConfig defaults = WwwConfigLoader.loadDefaults();
        ProfileDescriptor first = WonderfulWolfProfileFoundation.descriptor(defaults);
        ProfileDescriptor second = WonderfulWolfProfileFoundation.descriptor(defaults);

        assertEquals("wonderful-wolf", first.profileId());
        assertEquals(1, first.profileVersion());
        assertArrayEquals(first.semanticFingerprint(), second.semanticFingerprint());
        assertEquals(32, first.semanticFingerprint().length);

        WwwConfig changedRuntime = new WwwConfig(
                defaults.configVersion(),
                defaults.founderTarget(),
                defaults.genomeProfile(),
                new WwwConfig.Runtime(
                        defaults.runtime().combat(),
                        defaults.runtime().actionDistance(),
                        defaults.runtime().wanWand(),
                        defaults.runtime().age(),
                        defaults.runtime().relationship(),
                        new WwwConfig.PersonalityRuntime(0.20),
                        defaults.runtime().traits(),
                        defaults.runtime().spawn()));

        assertArrayEquals(
                first.semanticFingerprint(),
                WonderfulWolfProfileFoundation.descriptor(changedRuntime).semanticFingerprint());

        WwwConfig.GenomeProfile gp = defaults.genomeProfile();
        WwwConfig.Decoder d = gp.decoder();
        WwwConfig changedProfile = new WwwConfig(
                defaults.configVersion(),
                defaults.founderTarget(),
                new WwwConfig.GenomeProfile(
                        new WwwConfig.Decoder(
                                d.personality(),
                                new WwwConfig.TraitDecoder(0.55, d.trait().strongGap()),
                                d.injury(),
                                d.divine()),
                        gp.synthesizer(),
                        gp.breedingPolicy()),
                defaults.runtime());

        assertFalse(java.util.Arrays.equals(
                first.semanticFingerprint(),
                WonderfulWolfProfileFoundation.descriptor(changedProfile).semanticFingerprint()));
    }

    @Test
    void backboneDefinesSixSpecifiedChromosomesMarkersAndSafetyBoundary() {
        WwwConfig config = WwwConfigLoader.loadDefaults();
        BackboneDefinition backbone = WonderfulWolfProfileFoundation.backbone(config);

        assertEquals("wonderful-wolf", backbone.backboneId());
        assertEquals(1, backbone.genomeFormatVersion());
        assertEquals(List.of(9216, 8192, 7168, 6144, 5120, 4096),
                backbone.baselineChromosomeLengths());
        assertEquals(6, backbone.chromosomes().size());

        for (var chromosome : backbone.chromosomes()) {
            assertTrue(chromosome.anchors().size() >= 7);
            assertTrue(chromosome.markerLocus() != null);
            assertTrue(chromosome.anchors().contains(chromosome.markerLocus().first()));
            assertTrue(chromosome.anchors().contains(chromosome.markerLocus().second()));
            assertEquals(0.05, chromosome.founderScaffoldTolerance().standardDeviationRatio());
            assertEquals(0.90, chromosome.founderScaffoldTolerance().minLengthRatio());
            assertEquals(1.10, chromosome.founderScaffoldTolerance().maxLengthRatio());
        }

        assertTrue(backbone.safetyPolicy().isSafe(
                List.of(9216, 8192, 7168, 6144, 5120, 4096),
                backbone.baselineChromosomeLengths()));
        assertFalse(backbone.safetyPolicy().isSafe(
                List.of(2000, 8192, 7168, 6144, 5120, 4096),
                backbone.baselineChromosomeLengths()));

        assertNotEquals(
                backbone.chromosomes().get(0).anchors().get(0).canonicalBits(),
                backbone.chromosomes().get(0).anchors().get(1).canonicalBits());
    }
}
