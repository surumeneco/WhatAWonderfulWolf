package co.surumene.www.domain;

public enum Ability {
    HEALTH(0x00),
    DEFENSE(0x01),
    PATIENCE(0x02),
    SIZE(0x03),
    INVENTORY(0x04),
    MOVEMENT_SPEED(0x05),
    JUMP(0x06),
    STEP_HEIGHT(0x07),
    ATTACK_DAMAGE(0x08),
    ATTACK_SPEED(0x09);

    private final int targetId;

    Ability(int targetId) {
        this.targetId = targetId;
    }

    public int targetId() {
        return targetId;
    }
}
