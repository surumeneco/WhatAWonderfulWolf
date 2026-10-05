package co.surumene.www.spawn;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.founder.FounderGenomeSynthesis;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.founder.WonderfulWolfFounderSynthesizer;
import co.surumene.www.genome.PhenotypeOrigin;
import co.surumene.www.genome.WonderfulWolfDecodedPhenotype;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.PedigreeSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wgl.api.DecodeResult;
import co.surumene.wgl.api.GenomeEngine;
import co.surumene.wgl.api.SynthesisResult;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class WonderfulWolfFactory implements FounderIndividualSource {
    private final GenomeEngine engine;
    private final Supplier<WonderfulWolfGenomeProfile> profileSupplier;

    public WonderfulWolfFactory(
            GenomeEngine engine,
            Supplier<WonderfulWolfGenomeProfile> profileSupplier) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.profileSupplier =
                Objects.requireNonNull(profileSupplier, "profileSupplier");
    }

    @Override
    public WonderfulWolfCreationResult createFounder(
            FounderOrigin origin,
            Optional<UUID> ownerId,
            long adultBiologicalTime,
            long seed) {
        Objects.requireNonNull(origin, "origin");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        if (adultBiologicalTime < 0L) {
            throw new IllegalArgumentException(
                    "adultBiologicalTime must be >= 0");
        }

        WonderfulWolfGenomeProfile profile =
                Objects.requireNonNull(profileSupplier.get(), "current profile");
        FounderGenomeSynthesis synthesis =
                new WonderfulWolfFounderSynthesizer(engine, profile)
                        .synthesize(origin, seed);

        if (synthesis.result() instanceof SynthesisResult.Failure failure) {
            return new WonderfulWolfCreationResult.Failure(
                    failure.reason().name(),
                    failure.detail());
        }

        SynthesisResult.Success success =
                (SynthesisResult.Success) synthesis.result();
        DecodeResult<?> decoded = success.decoded();
        if (!(decoded.phenotype()
                instanceof WonderfulWolfDecodedPhenotype phenotype)) {
            return new WonderfulWolfCreationResult.Failure(
                    "NON_WWW_PHENOTYPE",
                    "Founder synthesis did not return a Wonderful Wolf phenotype");
        }

        PhenotypeSnapshot snapshot = phenotype.toSnapshot(
                decoded.identity(),
                origin == FounderOrigin.NATURAL
                        ? PhenotypeOrigin.NATURAL_FOUNDER
                        : PhenotypeOrigin.WOLF_TRAP_FOUNDER);

        return new WonderfulWolfCreationResult.Success(
                new WonderfulWolfIndividual(
                        success.genome(),
                        snapshot,
                        ownerId,
                        adultBiologicalTime,
                        Mode.WANDER,
                        Optional.empty(),
                        ActionDistance.NORMAL,
                        Optional.empty(),
                        Map.of(),
                        Optional.empty(),
                        Map.of(),
                        0,
                        PedigreeSnapshot.founder()));
    }
}
