package co.surumene.www.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Wolf;

import java.util.Objects;

/** Resolve player-defined Wolf names without exposing the vanilla entity name as a default. */
public final class WolfDisplayName {
    private WolfDisplayName() {}

    public static Component fromWolf(Wolf wolf, Component fallback) {
        Objects.requireNonNull(wolf, "wolf");
        return resolve(wolf.customName(), fallback);
    }

    public static Component resolve(Component customName, Component fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return customName == null
                || PlainTextComponentSerializer.plainText().serialize(customName).isBlank()
                ? fallback : customName;
    }
}
