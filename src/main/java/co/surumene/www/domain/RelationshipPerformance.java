package co.surumene.www.domain;

public record RelationshipPerformance(int initialAffection, int affectionDelta) {
    public RelationshipPerformance {
        if (affectionDelta < 0) {
            throw new IllegalArgumentException("affectionDelta must be >= 0");
        }
    }
}
