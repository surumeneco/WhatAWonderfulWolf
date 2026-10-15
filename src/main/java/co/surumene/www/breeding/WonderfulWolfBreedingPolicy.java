package co.surumene.www.breeding;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.wgl.api.GenomeAddress;
import co.surumene.wgl.api.InheritanceConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WonderfulWolfBreedingPolicy {
    private static final GenomeAddress DIVINE_ADDRESS = new GenomeAddress(0x05, 0x00);

    private WonderfulWolfBreedingPolicy() {}

    public static Set<GenomeAddress> deNovoForbiddenAddresses() {
        return Set.of(DIVINE_ADDRESS);
    }

    public static double mutationMultiplier(
            PhenotypeSnapshot parentA,
            PhenotypeSnapshot parentB,
            WwwConfig.BreedingPolicy policy) {
        Objects.requireNonNull(parentA, "parentA");
        Objects.requireNonNull(parentB, "parentB");
        Objects.requireNonNull(policy, "policy");
        return wildMultiplier(parentA, policy) * wildMultiplier(parentB, policy);
    }

    public static double directRetentionProbability(
            TraitStrength strength,
            WwwConfig.BreedingPolicy policy) {
        Objects.requireNonNull(strength, "strength");
        Objects.requireNonNull(policy, "policy");
        double weak = policy.directInheritance().weakPreferProbability();
        return strength == TraitStrength.STRONG ? Math.min(1.0, weak * 2.0) : weak;
    }

    public static List<InheritanceConstraint> resolveSoftAgainstHard(
            List<InheritanceConstraint> hard,
            List<InheritanceConstraint> soft) {
        List<InheritanceConstraint> hardCopy = List.copyOf(Objects.requireNonNull(hard, "hard"));
        List<InheritanceConstraint> result = new ArrayList<>();
        for (InheritanceConstraint candidate : Objects.requireNonNull(soft, "soft")) {
            Objects.requireNonNull(candidate, "soft constraint");
            boolean conflict = hardCopy.stream().anyMatch(h -> overlaps(h, candidate));
            if (!conflict) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    public static Optional<TraitStrength> expressedTrait(
            PhenotypeSnapshot snapshot,
            Trait trait) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(trait, "trait");
        return snapshot.expressedTraits().stream()
                .filter(entry -> entry.trait() == trait)
                .map(ExpressedTrait::strength)
                .findFirst();
    }

    private static double wildMultiplier(
            PhenotypeSnapshot snapshot,
            WwwConfig.BreedingPolicy policy) {
        return expressedTrait(snapshot, Trait.WILD)
                .map(strength -> strength == TraitStrength.STRONG
                        ? policy.wildTrait().strongParentMultiplier()
                        : policy.wildTrait().weakParentMultiplier())
                .orElse(1.0);
    }

    private static boolean overlaps(
            InheritanceConstraint a,
            InheritanceConstraint b) {
        return a.chromosomeIndex() == b.chromosomeIndex()
                && a.startBit() < b.endBitExclusive()
                && b.startBit() < a.endBitExclusive();
    }
}
