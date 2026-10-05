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
        return false;
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
