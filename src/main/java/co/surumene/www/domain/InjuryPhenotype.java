package co.surumene.www.domain;

import java.util.Objects;

public record InjuryPhenotype(Ability ability, double onsetGameDay, double severityRank) {
    public InjuryPhenotype {
        Objects.requireNonNull(ability, "ability");
        if (!Double.isFinite(onsetGameDay) || onsetGameDay < 0.0) {
            throw new IllegalArgumentException("onsetGameDay must be finite and >= 0");
        }
        if (!Double.isFinite(severityRank) || severityRank < 0.0) {
            throw new IllegalArgumentException("severityRank must be finite and >= 0");
        }
    }
}
