package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.wgl.api.GenomeAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WonderfulWolfContributionModelTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void usesStandardSaturationForNormalAddressesAndDoubleSaturationForExtraordinaryAbility() {
        GenomeAddress normal = new GenomeAddress(0x00, 0x00);
        GenomeAddress extraordinary = new GenomeAddress(0x07, 0x00);

        double normalEffect = profile.contributionModel(normal)
                .baseEffect(normal, false, 127, 15);
        double extraordinaryEffect = profile.contributionModel(extraordinary)
                .baseEffect(extraordinary, false, 127, 15);

        assertEquals(normalEffect, extraordinaryEffect, 1.0e-15);
        assertEquals(0.30,
                profile.contributionModel(normal).saturation(normal, normalEffect),
                1.0e-12);
        assertEquals(0.51,
                profile.contributionModel(extraordinary).saturation(extraordinary, extraordinaryEffect),
                1.0e-12);
    }

    @Test
    void rejectsContributionModelLookupForUndefinedAddress() {
        assertThrows(IllegalArgumentException.class,
                () -> profile.contributionModel(new GenomeAddress(0x00, 0x0A)));
    }
}
