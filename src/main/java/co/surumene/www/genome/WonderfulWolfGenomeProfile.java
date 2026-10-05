package co.surumene.www.genome;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigValidator;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.wgl.api.BackboneDefinition;
import co.surumene.wgl.api.DecodedGenome;
import co.surumene.wgl.api.DirectContributionModel;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.GenomeProfile;
import co.surumene.wgl.api.ProfileDescriptor;

import java.util.Objects;

/**
 * Immutable Wonderful Wolf WGL profile boundary.
 *
 * Phase 1 owns lifecycle, identity and Backbone state. Address semantics and
 * phenotype mapping are implemented in Phase 2.
 */
public final class WonderfulWolfGenomeProfile implements GenomeProfile<PhenotypeSnapshot> {
    private final WwwConfig config;
    private final ProfileDescriptor descriptor;
    private final BackboneDefinition backbone;

    public WonderfulWolfGenomeProfile(WwwConfig config) {
        this.config = WwwConfigValidator.validate(Objects.requireNonNull(config, "config"));
        this.descriptor = WonderfulWolfProfileFoundation.descriptor(this.config);
        this.backbone = WonderfulWolfProfileFoundation.backbone(this.config);
    }

    public WwwConfig config() {
        return config;
    }

    public BackboneDefinition backbone() {
        return backbone;
    }

    @Override
    public ProfileDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public boolean isDefinedAddress(GenomeAddress address) {
        Objects.requireNonNull(address, "address");
        return switch (address.type()) {
            case 0x00 -> address.target() <= 0x09;
            case 0x01 -> address.target() <= 0x04;
            case 0x02 -> address.target() <= 0x09;
            case 0x03 -> address.target() <= 0x05;
            case 0x04 -> address.target() <= 0x11;
            case 0x05 -> address.target() == 0x00;
            case 0x06 -> address.target() <= 0x01;
            case 0x07 -> address.target() <= 0x09;
            default -> false;
        };
    }

    @Override
    public int minimumExtensionBits(GenomeAddress address) {
        Objects.requireNonNull(address, "address");
        return address.type() == 0x02 && address.target() <= 0x09 ? 16 : 0;
    }

    @Override
    public DirectContributionModel contributionModel(GenomeAddress address) {
        throw new IllegalArgumentException("Wonderful Wolf decoder addresses are not available before Phase 2");
    }

    @Override
    public PhenotypeSnapshot mapPhenotype(DecodedGenome decodedGenome) {
        throw new IllegalStateException("Wonderful Wolf phenotype decoder is not available before Phase 2");
    }
}
