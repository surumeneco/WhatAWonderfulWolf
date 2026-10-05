package co.surumene.www.domain;

public enum Trait {
    MINING_SUPPORT(0x00),
    MUSCLE(0x01),
    GUARDIAN(0x02),
    HEALING(0x03),
    DIRECT_INHERITANCE(0x04),
    WILD(0x05),
    WATCHMAN(0x06),
    HOLY_POISON(0x07),
    QUIRK_BOOST(0x08),
    UNYIELDING(0x09),
    SCAVENGER(0x0A),
    LIFE_DRAIN(0x0B),
    FIRE_BIRD(0x0C),
    INTIMIDATION(0x0D),
    LIGHTWEIGHT(0x0E),
    TARGET(0x0F),
    SWIMMER(0x10),
    SNOWBORN(0x11);

    private final int targetId;

    Trait(int targetId) {
        this.targetId = targetId;
    }

    public int targetId() {
        return targetId;
    }
}
