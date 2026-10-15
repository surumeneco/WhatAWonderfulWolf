package co.surumene.www.individual;

import java.util.Objects;
import java.util.Optional;

public record PedigreeSnapshot(
        Optional<ParentSnapshot> parentA,
        Optional<ParentSnapshot> parentB,
        Optional<AncestorSnapshot> grandparentAA,
        Optional<AncestorSnapshot> grandparentAB,
        Optional<AncestorSnapshot> grandparentBA,
        Optional<AncestorSnapshot> grandparentBB) {
    public PedigreeSnapshot {
        parentA = require(parentA, "parentA");
        parentB = require(parentB, "parentB");
        grandparentAA = require(grandparentAA, "grandparentAA");
        grandparentAB = require(grandparentAB, "grandparentAB");
        grandparentBA = require(grandparentBA, "grandparentBA");
        grandparentBB = require(grandparentBB, "grandparentBB");

        if (parentA.isEmpty() && (grandparentAA.isPresent() || grandparentAB.isPresent())) {
            throw new IllegalArgumentException("parent A grandparents require parent A");
        }
        if (parentB.isEmpty() && (grandparentBA.isPresent() || grandparentBB.isPresent())) {
            throw new IllegalArgumentException("parent B grandparents require parent B");
        }
    }

    public static PedigreeSnapshot founder() {
        return new PedigreeSnapshot(
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static <T> Optional<T> require(Optional<T> value, String name) {
        return Objects.requireNonNull(value, name);
    }
}
