package co.surumene.www.domain;

public enum Mode {
    WANDER("放浪"),
    FOLLOW("追従"),
    GUARD("護衛"),
    WAIT("待機");

    private final String displayName;

    Mode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
