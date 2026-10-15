package co.surumene.www.runtime;

import co.surumene.www.domain.Trait;
import org.bukkit.potion.PotionEffectType;

final class MinecraftTriggeredEffectTypes {
    private MinecraftTriggeredEffectTypes() {}

    static PotionEffectType forTrait(Trait trait) {
        return switch (trait) {
            case INTIMIDATION -> PotionEffectType.WEAKNESS;
            case HOLY_POISON -> PotionEffectType.POISON;
            default -> null;
        };
    }
}
