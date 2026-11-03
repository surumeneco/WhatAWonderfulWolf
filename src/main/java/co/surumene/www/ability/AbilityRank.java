package co.surumene.www.ability;

public enum AbilityRank {
    MISERABLE("miserable", "劣悪"),
    VERY_LOW("very_low", "とても低い"),
    LOW("low", "低い"),
    SLIGHTLY_LOW("slightly_low", "やや低い"),
    COMMON("common", "普通"),
    SLIGHTLY_HIGH("slightly_high", "やや高い"),
    HIGH("high", "高い"),
    VERY_HIGH("very_high", "とても高い"),
    LEGENDARY("legendary", "伝説級"),
    MYTHICAL("mythical", "神話級"),
    IMPOSSIBLE("impossible", "規格外");

    private final String key;
    private final String displayName;

    AbilityRank(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public static AbilityRank fromNormalized(double normalized) {
        return values()[co.surumene.wcl.api.ability.NormalizedRankIndex.of(normalized, true)];
    }}
