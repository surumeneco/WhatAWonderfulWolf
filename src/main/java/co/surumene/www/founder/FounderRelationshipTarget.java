package co.surumene.www.founder;

public record FounderRelationshipTarget(
        double initialAffinityScore,
        double affinityChangeScore) {

    public FounderRelationshipTarget {
        validate(initialAffinityScore, "initialAffinityScore");
        validate(affinityChangeScore, "affinityChangeScore");
    }

    private static void validate(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be finite and in [0,1]");
        }
    }
}
