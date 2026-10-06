package co.surumene.www.domain;

public enum Ability {
    HEALTH(0x00, "体力"),
    DEFENSE(0x01, "基礎防御力"),
    PATIENCE(0x02, "忍耐力"),
    SIZE(0x03, "大きさ"),
    INVENTORY(0x04, "積載量"),
    MOVEMENT_SPEED(0x05, "移動速度"),
    JUMP(0x06, "ジャンプ力"),
    STEP_HEIGHT(0x07, "段差無視"),
    ATTACK_DAMAGE(0x08, "基礎攻撃力"),
    ATTACK_SPEED(0x09, "基礎攻撃速度");

    private final int targetId;
    private final String displayName;

    Ability(int targetId, String displayName) {
        this.targetId = targetId;
        this.displayName = displayName;
    }

    public int targetId() {
        return targetId;
    }

    public String displayName() {
        return displayName;
    }
}
