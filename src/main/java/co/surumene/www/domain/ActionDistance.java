package co.surumene.www.domain;

import co.surumene.www.config.WwwConfig;

public enum ActionDistance {
    NARROW,
    NORMAL,
    WIDE,
    VERY_WIDE;

    public double blocks(WwwConfig.ActionDistance config) {
        return switch (this) {
            case NARROW -> config.narrowBlocks();
            case NORMAL -> config.normalBlocks();
            case WIDE -> config.wideBlocks();
            case VERY_WIDE -> config.veryWideBlocks();
        };
    }
}
