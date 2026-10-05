package co.surumene.www.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class WwwConfigValidationTest {
    @Test
    void rejectsDescendingActionDistanceThresholds() {
        WwwConfig valid = WwwConfigLoader.loadDefaults();
        WwwConfig.Runtime runtime = valid.runtime();
        WwwConfig invalid = new WwwConfig(
                valid.configVersion(),
                valid.founderTarget(),
                valid.genomeProfile(),
                new WwwConfig.Runtime(
                        runtime.combat(),
                        new WwwConfig.ActionDistance(5.0, 4.0, 20.0, 30.0),
                        runtime.wanWand(),
                        runtime.age(),
                        runtime.relationship(),
                        runtime.personality(),
                        runtime.traits(),
                        runtime.spawn()));

        assertThrows(IllegalArgumentException.class, () -> WwwConfigValidator.validate(invalid));
    }
}
