package co.surumene.www.breeding;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.genome.PhenotypeOrigin;
import co.surumene.www.genome.WonderfulWolfDecodedPhenotype;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class WonderfulWolfBreedingService {
    private final GenomeEngine engine;
    private final Supplier<WonderfulWolfGenomeProfile> profileSupplier;

    public WonderfulWolfBreedingService(
            GenomeEngine engine,
            Supplier<WonderfulWolfGenomeProfile> profileSupplier) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.profileSupplier =
                Objects.requireNonNull(profileSupplier, "profileSupplier");
    }

    public WonderfulWolfBreedingOutcome breed(
            BreedingParent parentA,
            BreedingParent parentB,
            Optional<UUID> childOwnerId,
            long seed) {
        return breed(
                parentA,
                parentB,
                childOwnerId,
                engine.standardRandom(seed));
    }

    public WonderfulWolfBreedingOutcome breed(
            BreedingParent parentA,
            BreedingParent parentB,
            Optional<UUID> childOwnerId,
            GenomeRandom random) {
        Objects.requireNonNull(parentA, "parentA");
        Objects.requireNonNull(parentB, "parentB");
        childOwnerId = Objects.requireNonNull(childOwnerId, "childOwnerId");
        Objects.requireNonNull(random, "random");

        WonderfulWolfGenomeProfile profile =
                Objects.requireNonNull(profileSupplier.get(), "current profile");

        CompatibilityReport compatibility = engine.assessCompatibility(
                parentA.individual().genome(),
                parentB.individual().genome(),
                null);
        if (!compatibility.compatible()) {
            return new WonderfulWolfBreedingOutcome.Fallback(
                    "incompatible WWW parent genomes: " + compatibility.reason());
        }

        WonderfulWolfBreedingContextFactory contextFactory =
                new WonderfulWolfBreedingContextFactory(engine, profile);
        BreedingContext generatedContext = contextFactory.create(
                parentA.individual(),
                parentB.individual(),
                random);
        BreedingContext context = new BreedingContext(
                generatedContext.backbone(),
                generatedContext.mutationRateMultiplier(),
                generatedContext.deNovoForbiddenAddresses(),
                (ignoredA, ignoredB) -> compatibility,
                generatedContext.allowSafetyOverride(),
                generatedContext.parentAPolicy(),
                generatedContext.parentBPolicy());

        BreedingResult result = engine.breed(
                profile,
                parentA.individual().genome(),
                parentB.individual().genome(),
                context,
                random);
        if (result instanceof BreedingResult.NoViableOffspring failure) {
            return new WonderfulWolfBreedingOutcome.Fallback(
                    failure.reason() + ": " + failure.detail());
        }

        BreedingResult.Success success =
                (BreedingResult.Success) result;
        DiploidGenome childGenome = success.genome();
        DecodeResult<?> canonical = success.decoded();
        if (!(canonical.phenotype() instanceof WonderfulWolfDecodedPhenotype phenotype)) {
            throw new IllegalStateException(
                    "WGL breeding returned a non-WWW phenotype");
        }
        PhenotypeSnapshot childSnapshot = phenotype.toSnapshot(
                canonical.identity(),
                PhenotypeOrigin.BREEDING);

        int generation = Math.max(
                parentA.individual().generation(),
                parentB.individual().generation()) + 1;
        PedigreeSnapshot pedigree = pedigree(parentA, parentB, profile);

        WonderfulWolfIndividual child = new WonderfulWolfIndividual(
                childGenome,
                childSnapshot,
                childOwnerId,
                0L,
                Mode.WANDER,
                Optional.empty(),
                ActionDistance.NORMAL,
                Optional.empty(),
                Map.of(),
                Optional.empty(),
                Map.of(),
                generation,
                pedigree);

        return new WonderfulWolfBreedingOutcome.Success(
                child,
                marker(profile, childGenome));
    }

    private PedigreeSnapshot pedigree(
            BreedingParent parentA,
            BreedingParent parentB,
            WonderfulWolfGenomeProfile profile) {
        return new PedigreeSnapshot(
                Optional.of(parentSnapshot(parentA, profile)),
                Optional.of(parentSnapshot(parentB, profile)),
                grandparent(parentA.individual().pedigree().parentA()),
                grandparent(parentA.individual().pedigree().parentB()),
                grandparent(parentB.individual().pedigree().parentA()),
                grandparent(parentB.individual().pedigree().parentB()));
    }

    private ParentSnapshot parentSnapshot(
            BreedingParent parent,
            WonderfulWolfGenomeProfile profile) {
        WonderfulWolfIndividual individual = parent.individual();
        PhenotypeSnapshot phenotype = individual.phenotypeSnapshot();

        AncestorSnapshot ancestor = new AncestorSnapshot(
                parent.displayName(),
                individual.generation(),
                marker(profile, individual.genome()));

        List<String> traits = phenotype.expressedTraits().stream()
                .map(ExpressedTrait::trait)
                .map(Enum::name)
                .toList();

        return new ParentSnapshot(
                ancestor,
                phenotype.personality().name(),
                traits,
                phenotype.divineLineageExpressed());
    }

    private static Optional<AncestorSnapshot> grandparent(
            Optional<ParentSnapshot> parent) {
        return parent.map(ParentSnapshot::ancestor);
    }

    private String marker(
            WonderfulWolfGenomeProfile profile,
            DiploidGenome genome) {
        return engine.marker(profile.backbone(), genome).formatted();
    }
}
