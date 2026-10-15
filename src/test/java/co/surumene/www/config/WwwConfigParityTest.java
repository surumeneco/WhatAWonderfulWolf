package co.surumene.www.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class WwwConfigParityTest {
    private static YamlConfiguration defaults() {
        var stream = WwwConfigParityTest.class.getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(stream);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Test void sharedWwcStyleKeysAreOnlyDistributedKeys() {
        var yaml = defaults();
        for (String key : Set.of("natural-spawn.chance", "nature.adjustment",
                "traits.alert-min-interval-ticks", "traits.alert-max-interval-ticks",
                "wolf-trap.spawn-chance", "wolf-trap.lightning-chance",
                "wolf-trap.activation-radius-blocks",
                "runtime.age.sensitivity.max-health", "runtime.age.sensitivity.ground-speed",
                "runtime.age.sensitivity.jump-strength")) {
            assertTrue(yaml.contains(key), key);
        }
        for (String old : Set.of("runtime.spawn.natural-conversion-probability",
                "runtime.spawn.wolf-trap-lightning-probability",
                "runtime.spawn.wolf-trap-activation-radius-blocks",
                "runtime.personality.modifier-rate",
                "runtime.traits.watchman-warning-min-ticks",
                "runtime.age.sensitivity.health",
                "runtime.age.sensitivity.movement-speed",
                "runtime.age.sensitivity.jump")) {
            assertFalse(yaml.contains(old), old);
        }
    }

    @Test void newSpawnProbabilityIsActuallyLoadedAndValidated() {
        var yaml = defaults();
        yaml.set("wolf-trap.spawn-chance", 0.27);
        assertEquals(0.27, WwwConfigLoader.load(yaml).runtime().spawn().wolfTrapSpawnChance());
        yaml.set("wolf-trap.spawn-chance", 1.1);
        assertThrows(IllegalArgumentException.class, () -> WwwConfigLoader.load(yaml));
    }
}
