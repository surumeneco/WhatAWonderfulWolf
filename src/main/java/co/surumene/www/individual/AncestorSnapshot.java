package co.surumene.www.individual;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Birth-time identity and optional expressed phenotype of an ancestor.
 * Three-field constructor and version 1/2 saved records remain compatible.
 */
public record AncestorSnapshot(
        String displayName, int generation, String lineageId,
        Optional<String> personalityId, List<String> expressedTraitIds) {

    public AncestorSnapshot(String displayName, int generation, String lineageId) {
        this(displayName, generation, lineageId, Optional.empty(), List.of());
    }

    public AncestorSnapshot {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(lineageId, "lineageId");
        personalityId = Objects.requireNonNull(personalityId, "personalityId");
        expressedTraitIds = List.copyOf(
                Objects.requireNonNull(expressedTraitIds, "expressedTraitIds"));
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (generation < 0) throw new IllegalArgumentException("generation must be >= 0");
        if (lineageId.isBlank()) throw new IllegalArgumentException("lineageId must not be blank");
        if (personalityId.isPresent() && personalityId.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("personalityId must not be blank");
        }
        if (expressedTraitIds.size() > 2) {
            throw new IllegalArgumentException("ancestor has too many expressed traits");
        }
        for (String id : expressedTraitIds) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("blank ancestor trait id");
            }
        }
    }

    public AncestorSnapshot withPhenotype(String personality, List<String> traits) {
        return new AncestorSnapshot(displayName, generation, lineageId,
                Optional.of(personality), traits);
    }
}
