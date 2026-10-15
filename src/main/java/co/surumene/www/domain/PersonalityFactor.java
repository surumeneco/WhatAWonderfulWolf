package co.surumene.www.domain;

public enum PersonalityFactor {
    AGILITY(0x00),
    LEG_STRENGTH(0x01),
    ROBUSTNESS(0x02),
    FIGHTING(0x03),
    PHYSIQUE(0x04),
    NEUTRAL(0x05);

    private final int targetId;

    PersonalityFactor(int targetId) {
        this.targetId = targetId;
    }

    public int targetId() {
        return targetId;
    }
}
