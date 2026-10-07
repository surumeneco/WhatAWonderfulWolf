package co.surumene.www.breeding;

import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.wgl.api.*;

import java.util.Objects;
import java.util.function.Supplier;

public final class WonderfulWolfOffspringService {
    private final GenomeEngine engine;
    private final Supplier<WonderfulWolfGenomeProfile> profileSupplier;

    public WonderfulWolfOffspringService(
            GenomeEngine engine,
            Supplier<WonderfulWolfGenomeProfile> profileSupplier) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.profileSupplier =
                Objects.requireNonNull(profileSupplier, "profileSupplier");
    }

    public Result breed(
            BreedingParentSource parentA,
            BreedingParentSource parentB,
            long seed) {
        return breed(parentA, parentB, engine.standardRandom(seed));
    }

    public Result breed(
            BreedingParentSource parentA,
            BreedingParentSource parentB,
            GenomeRandom random) {
        Objects.requireNonNull(parentA, "parentA");
        Objects.requireNonNull(parentB, "parentB");
        Objects.requireNonNull(random, "random");

        WonderfulWolfGenomeProfile profile =
                Objects.requireNonNull(
                        profileSupplier.get(),
                        "current profile");

        BackboneCompatibilityReport compatibilityA =
                assessBackbone(profile.backbone(), parentA);
        if (!compatibilityA.compatible()) {
            return new Result.Failure(
                    "BACKBONE_INCOMPATIBLE_A",
                    compatibilityA.reason());
        }

        BackboneCompatibilityReport compatibilityB =
                assessBackbone(profile.backbone(), parentB);
        if (!compatibilityB.compatible()) {
            return new Result.Failure(
                    "BACKBONE_INCOMPATIBLE_B",
                    compatibilityB.reason());
        }

        BreedingContext context = new BreedingContext(
                profile.backbone(),
                1.0,
                WonderfulWolfBreedingPolicy.deNovoForbiddenAddresses(),
                null,
                false);

        BreedingResult result =
                engine.breed(
                        profile,
                        parentA,
                        parentB,
                        context,
                        random);
        if (result instanceof BreedingResult.NoViableOffspring failure) {
            return new Result.Failure(
                    failure.reason().name(),
                    failure.detail());
        }

        BreedingResult.Success success =
                (BreedingResult.Success) result;
        return new Result.Success(success.genome());
    }

    private BackboneCompatibilityReport assessBackbone(
            BackboneDefinition backbone,
            BreedingParentSource source) {
        if (source instanceof BreedingParentSource.DiploidParent diploid) {
            return engine.assessBackboneCompatibility(
                    backbone,
                    diploid.genome());
        }
        return engine.assessBackboneCompatibility(
                backbone,
                ((BreedingParentSource.Gamete) source).genome());
    }

    public sealed interface Result
            permits Result.Success, Result.Failure {
        record Success(DiploidGenome genome) implements Result {
            public Success {
                Objects.requireNonNull(genome, "genome");
            }
        }

        record Failure(String reason, String detail) implements Result {
            public Failure {
                Objects.requireNonNull(reason, "reason");
                detail = detail == null ? "" : detail;
            }
        }
    }
}
