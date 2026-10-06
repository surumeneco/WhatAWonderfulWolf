package co.surumene.www.domain;

public enum Trait {
    MINING_SUPPORT(0x00, "採掘応援"),
    MUSCLE(0x01, "力こぶ"),
    GUARDIAN(0x02, "守護"),
    HEALING(0x03, "癒やし"),
    DIRECT_INHERITANCE(0x04, "直伝"),
    WILD(0x05, "破天荒"),
    WATCHMAN(0x06, "見張り番"),
    HOLY_POISON(0x07, "聖毒"),
    QUIRK_BOOST(0x08, "クセ増し"),
    UNYIELDING(0x09, "不屈"),
    SCAVENGER(0x0A, "拾い食い"),
    LIFE_DRAIN(0x0B, "吸命"),
    FIRE_BIRD(0x0C, "火の鳥"),
    INTIMIDATION(0x0D, "威嚇"),
    LIGHTWEIGHT(0x0E, "身軽"),
    TARGET(0x0F, "標的"),
    SWIMMER(0x10, "泳ぎ上手"),
    SNOWBORN(0x11, "雪国生まれ");

    private final int targetId;
    private final String displayName;

    Trait(int targetId, String displayName) {
        this.targetId = targetId;
        this.displayName = displayName;
    }

    public int targetId() {
        return targetId;
    }

    public String displayName() {
        return displayName;
    }

    public static Trait fromTargetId(int targetId) {
        for (Trait trait : values()) {
            if (trait.targetId == targetId) return trait;
        }
        throw new IllegalArgumentException("unknown trait targetId: " + targetId);
    }
}
