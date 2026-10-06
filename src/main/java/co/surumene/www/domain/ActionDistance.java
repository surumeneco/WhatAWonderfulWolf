package co.surumene.www.domain;

import co.surumene.www.config.WwwConfig;

public enum ActionDistance {
    NARROW("狭い"),
    NORMAL("普通"),
    WIDE("広い"),
    VERY_WIDE("とても広い");

    private final String displayName;

    ActionDistance(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public double blocks(WwwConfig.ActionDistance config) {
        return switch (this) {
            case NARROW -> config.narrowBlocks();
            case NORMAL -> config.normalBlocks();
            case WIDE -> config.wideBlocks();
            case VERY_WIDE -> config.veryWideBlocks();
        };
    }
}
