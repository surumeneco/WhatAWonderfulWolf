package co.surumene.www.config;

import co.surumene.www.ability.AbilityScale;
import co.surumene.www.domain.Ability;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WwwStatsConfigurationTest {
    @Test void allTenDefaultAbilitiesPreserveCurrentProjection() {
        WwwConfig config = WwwConfigLoader.loadDefaults();
        assertEquals(Ability.values().length, config.runtime().stats().abilities().size());
        assertEquals(60.0, AbilityScale.toCanonical(Ability.HEALTH, 1.0, config.runtime().stats()));
        assertEquals(24.0, AbilityScale.toCanonical(Ability.MOVEMENT_SPEED, 1.0, config.runtime().stats()));
        assertEquals(30.0, AbilityScale.toCanonical(Ability.INVENTORY, 1.0, config.runtime().stats()));
    }

    @Test void configuredRangesApplyToCanonicalProjectionAndExtrapolate() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.setDefaults(YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(getClass().getClassLoader().getResourceAsStream("config.yml"))));
        yaml.set("stats.max-health.min", 24.0);
        yaml.set("stats.max-health.max", 64.0);
        WwwConfig config = WwwConfigLoader.load(yaml);
        assertEquals(24.0, AbilityScale.toCanonical(Ability.HEALTH, 0.0, config.runtime().stats()));
        assertEquals(64.0, AbilityScale.toCanonical(Ability.HEALTH, 1.0, config.runtime().stats()));
        assertEquals(84.0, AbilityScale.toCanonical(Ability.HEALTH, 1.5, config.runtime().stats()));
    }

    @Test void malformedRangesAndUnknownDisplayAreRejected() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(getClass().getClassLoader().getResourceAsStream("config.yml")));
        yaml.set("stats.max-health.max", 10);
        assertThrows(IllegalArgumentException.class, () -> WwwConfigLoader.load(yaml));
        yaml.set("stats.max-health.max", 60);
        yaml.set("stats.max-health.display", "undefined");
        assertThrows(IllegalArgumentException.class, () -> WwwConfigLoader.load(yaml));
    }

    @Test void oldConfigurationWithoutStatsRemainsReadable() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(getClass().getClassLoader().getResourceAsStream("config.yml")));
        yaml.set("stats", null);
        assertEquals(60.0, AbilityScale.toCanonical(Ability.HEALTH, 1.0,
                WwwConfigLoader.load(yaml).runtime().stats()));
    }
}
