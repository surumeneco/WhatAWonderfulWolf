package co.surumene.www.individual;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record ParentSnapshot(
        AncestorSnapshot ancestor,
        String personalityId,
        List<String> expressedTraitIds,
        boolean divineLineageExpressed) {
    public ParentSnapshot {
        Objects.requireNonNull(ancestor, "ancestor");
        Objects.requireNonNull(personalityId, "personalityId");
        if (personalityId.isBlank()) throw new IllegalArgumentException("personalityId must not be blank");
        expressedTraitIds = List.copyOf(Objects.requireNonNull(expressedTraitIds, "expressedTraitIds"));
        if (expressedTraitIds.size() > 2) {
            throw new IllegalArgumentException("expressedTraitIds must contain at most two traits");
        }
        HashSet<String> unique = new HashSet<>();
        for (String id : expressedTraitIds) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("trait id must not be blank");
            if (!unique.add(id)) throw new IllegalArgumentException("expressedTraitIds must be unique");
        }
    }
}
