package co.surumene.www.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WwwConfigValidationTest {
    @Test
    void distributedDefaultsAreValid() {
        assertDoesNotThrow(WwwConfigLoader::loadDefaults);
    }

    @Test
    void rejectsInvalidConfigurationInsteadOfPublishingPartialState() {
        assertInvalid(config -> config.set("runtime.spawn.natural-conversion-probability", 1.01));
        assertInvalid(config -> config.set("runtime.relationship.passive-increase-probability", Double.NaN));
        assertInvalid(config -> config.set("runtime.action-distance.normal-blocks", 4.0));
        assertInvalid(config -> config.set("runtime.traits.watchman-warning-min-ticks", 81));
        assertInvalid(config -> config.set("runtime.age.base-growth-game-days", 0.0));
        assertInvalid(config -> config.set("runtime.age.base-peak-duration-game-days", -0.01));
        assertInvalid(config -> config.set("runtime.age.base-aging-duration-game-days", 0.0));
        assertInvalid(config -> config.set("runtime.age.sensitivity.health", -0.01));
        assertInvalid(config -> config.set("founder-target.natural.abilities.standard-deviation", 0.0));
        assertInvalid(config -> config.set("founder-target.personality.standard-deviation", 0.0));
        assertInvalid(config -> config.set("genome-profile.decoder.personality.serious-spread", 1.01));
        assertInvalid(config -> config.set("genome-profile.decoder.personality.dominant-gap", -0.01));
        assertInvalid(config -> config.set("genome-profile.decoder.trait.expression-threshold", 1.01));
        assertInvalid(config -> config.set("genome-profile.decoder.injury.onset-max-game-days", 0.0));
        assertInvalid(config -> config.set("genome-profile.decoder.injury.severity-rank-min", 6.01));
        assertInvalid(config -> config.set("genome-profile.synthesizer.injury.load-min", 0.44));
        assertInvalid(config -> config.set("genome-profile.synthesizer.injury.blocks-max", 0));
        assertInvalid(config -> config.set("genome-profile.synthesizer.injury.genes-per-block", 0));
        assertInvalid(config -> config.set("genome-profile.synthesizer.chromosome-length-standard-deviation-ratio", 0.0));
        assertInvalid(config -> config.set("genome-profile.synthesizer.chromosome-length-min-ratio", 1.01));
        assertInvalid(config -> config.set("genome-profile.synthesizer.chromosome-length-max-ratio", 0.99));
        assertInvalid(config -> config.set("genome-profile.synthesizer.genes-per-target.ability-center", 13));
        assertInvalid(config -> config.set("genome-profile.synthesizer.direct-genes-soft-max", 257));
        assertInvalid(config -> config.set("genome-profile.synthesizer.regulation-genes-max", 65));
        assertInvalid(config -> config.set("genome-profile.synthesizer.recognizable-region-max-ratio", 0.66));
        assertInvalid(config -> config.set("genome-profile.synthesizer.cancellation-min", 0.36));
        assertInvalid(config -> config.set("genome-profile.synthesizer.personality-cancellation-min", 0.51));
        assertInvalid(config -> config.set("genome-profile.synthesizer.relay.attachment-min-ratio", 0.09));
        assertInvalid(config -> config.set("genome-profile.breeding-policy.direct-inheritance.weak-prefer-probability", -0.01));
    }

    private static void assertInvalid(Consumer<YamlConfiguration> mutation) {
        YamlConfiguration configuration = defaults();
        mutation.accept(configuration);
        assertThrows(IllegalArgumentException.class, () -> WwwConfigLoader.load(configuration));
    }

    private static YamlConfiguration defaults() {
        var stream = WwwConfigValidationTest.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new AssertionError("config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
