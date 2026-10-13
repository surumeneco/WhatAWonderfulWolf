package co.surumene.www.runtime;

import co.surumene.www.ability.EffectiveAbilities;
import co.surumene.www.domain.Ability;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Wolf;

import java.util.Objects;
import java.util.logging.Logger;

public final class PaperAbilityProjector {
    private static final double MOVEMENT_ATTRIBUTE_BLOCKS_PER_SECOND = 42.157;

    private final Logger logger;

    public PaperAbilityProjector(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void project(Wolf wolf, EffectiveAbilities abilities) {
        Objects.requireNonNull(wolf, "wolf");
        Objects.requireNonNull(abilities, "abilities");

        double maxHealth = abilities.canonical(Ability.HEALTH);
        setAttribute(wolf, Attribute.MAX_HEALTH, maxHealth);
        setAttribute(wolf, Attribute.SCALE, abilities.canonical(Ability.SIZE));
        setAttribute(
                wolf,
                Attribute.MOVEMENT_SPEED,
                movementAttributeForBlocksPerSecond(
                        abilities.canonical(Ability.MOVEMENT_SPEED)));
        setAttribute(
                wolf,
                Attribute.JUMP_STRENGTH,
                jumpStrengthForHeight(abilities.canonical(Ability.JUMP)));
        setAttribute(
                wolf,
                Attribute.STEP_HEIGHT,
                abilities.canonical(Ability.STEP_HEIGHT));
        setAttribute(
                wolf,
                Attribute.ATTACK_DAMAGE,
                abilities.canonical(Ability.ATTACK_DAMAGE));
        projectAttackSpeed(wolf, abilities.canonical(Ability.ATTACK_SPEED));
        setAttribute(
                wolf,
                Attribute.ARMOR,
                abilities.canonical(Ability.DEFENSE));

        if (wolf.getHealth() > maxHealth) {
            wolf.setHealth(maxHealth);
        }
    }

    public static double movementAttributeForBlocksPerSecond(
            double blocksPerSecond) {
        if (!Double.isFinite(blocksPerSecond) || blocksPerSecond < 0.0) {
            throw new IllegalArgumentException(
                    "blocksPerSecond must be finite and >= 0");
        }
        return blocksPerSecond / MOVEMENT_ATTRIBUTE_BLOCKS_PER_SECOND;
    }

    public static double blocksPerSecondForMovementAttribute(
            double movementAttribute) {
        if (!Double.isFinite(movementAttribute) || movementAttribute < 0.0) {
            throw new IllegalArgumentException(
                    "movementAttribute must be finite and >= 0");
        }
        return movementAttribute * MOVEMENT_ATTRIBUTE_BLOCKS_PER_SECOND;
    }

    public static double jumpStrengthForHeight(double height) {
        if (!Double.isFinite(height) || height < 0.0) {
            throw new IllegalArgumentException(
                    "height must be finite and >= 0");
        }
        return 0.42 * Math.sqrt(height / 1.25);
    }

    public static double heightForJumpStrength(double jumpStrength) {
        if (!Double.isFinite(jumpStrength) || jumpStrength < 0.0) {
            throw new IllegalArgumentException(
                    "jumpStrength must be finite and >= 0");
        }
        double ratio = jumpStrength / 0.42;
        return 1.25 * ratio * ratio;
    }

    /**
     * Register missing Wolf attack speed before projecting the phenotype base value.
     * Item and external modifiers remain attached to the same attribute instance.
     */
    void projectAttackSpeed(Wolf wolf, double baseAttacksPerSecond) {
        Objects.requireNonNull(wolf, "wolf");
        projectAttackSpeedSlot(
                () -> wolf.getAttribute(Attribute.ATTACK_SPEED) != null,
                () -> wolf.registerAttribute(Attribute.ATTACK_SPEED),
                value -> setAttribute(wolf, Attribute.ATTACK_SPEED, value),
                baseAttacksPerSecond,
                wolf.getUniqueId().toString());
    }

    /**
     * Small bootstrap-independent seam for testing the registration contract.
     */
    static void projectAttackSpeedSlot(
            java.util.function.BooleanSupplier available,
            Runnable register,
            java.util.function.DoubleConsumer apply,
            double baseAttacksPerSecond,
            String wolfId) {
        Objects.requireNonNull(available, "available");
        Objects.requireNonNull(register, "register");
        Objects.requireNonNull(apply, "apply");
        if (!available.getAsBoolean()) {
            try {
                register.run();
            } catch (RuntimeException error) {
                throw new IllegalStateException(
                        "Could not register attack_speed on Wonderful Wolf " + wolfId, error);
            }
            if (!available.getAsBoolean()) {
                throw new IllegalStateException(
                        "attack_speed unavailable after registration for Wonderful Wolf " + wolfId);
            }
        }
        apply.accept(baseAttacksPerSecond);
    }

    private void setAttribute(
            Wolf wolf,
            Attribute attribute,
            double value) {
        AttributeInstance instance = wolf.getAttribute(attribute);
        if (instance == null) {
            logger.fine(
                    "Attribute unavailable on Wonderful Wolf: "
                            + attribute.key().asString());
            return;
        }
        if (Double.compare(instance.getBaseValue(), value) == 0) {
            return;
        }
        try {
            instance.setBaseValue(value);
        } catch (IllegalArgumentException error) {
            logger.warning(
                    "Could not apply "
                            + attribute.key().asString()
                            + "="
                            + value
                            + " to Wonderful Wolf "
                            + wolf.getUniqueId()
                            + ": "
                            + error.getMessage());
            // Report invalid Paper attribute values to the caller.
            throw error;
        }
    }
}
