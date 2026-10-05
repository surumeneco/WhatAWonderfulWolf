package co.surumene.www.genome;

import co.surumene.www.config.WwwConfigLoader;
import co.surumene.wgl.api.GenomeAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WonderfulWolfAddressMapTest {
    private final WonderfulWolfGenomeProfile profile =
            new WonderfulWolfGenomeProfile(WwwConfigLoader.loadDefaults());

    @Test
    void definesOnlyWonderfulWolfConsumerAddresses() {
        assertRange(0x00, 0x00, 0x09);
        assertRange(0x01, 0x00, 0x04);
        assertRange(0x02, 0x00, 0x09);
        assertRange(0x03, 0x00, 0x05);
        assertRange(0x04, 0x00, 0x11);
        assertRange(0x05, 0x00, 0x00);
        assertRange(0x06, 0x00, 0x01);
        assertRange(0x07, 0x00, 0x09);

        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x00, 0x0A)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x01, 0x05)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x04, 0x12)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x05, 0x01)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x06, 0x02)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x07, 0x0A)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x08, 0x00)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0x00, 0xFF)));
        assertFalse(profile.isDefinedAddress(new GenomeAddress(0xF0, 0x00)));
    }

    @Test
    void injuryAddressesRequireSixteenExtensionBits() {
        for (int target = 0; target <= 0x09; target++) {
            assertEquals(16, profile.minimumExtensionBits(new GenomeAddress(0x02, target)));
        }
        assertEquals(0, profile.minimumExtensionBits(new GenomeAddress(0x00, 0x00)));
        assertEquals(0, profile.minimumExtensionBits(new GenomeAddress(0x07, 0x09)));
    }

    private void assertRange(int type, int firstTarget, int lastTarget) {
        for (int target = firstTarget; target <= lastTarget; target++) {
            GenomeAddress address = new GenomeAddress(type, target);
            assertTrue(profile.isDefinedAddress(address),
                    "expected defined address " + address);
        }
    }
}
