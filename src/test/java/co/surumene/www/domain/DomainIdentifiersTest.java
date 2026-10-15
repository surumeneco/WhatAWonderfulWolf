package co.surumene.www.domain;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DomainIdentifiersTest {
    @Test
    void exposesStableSpecificationIdentifiers() {
        assertEquals(0x00, Ability.HEALTH.targetId());
        assertEquals(0x09, Ability.ATTACK_SPEED.targetId());
        assertEquals(0x00, Trait.MINING_SUPPORT.targetId());
        assertEquals(0x11, Trait.SNOWBORN.targetId());
        assertEquals(0x00, PersonalityFactor.AGILITY.targetId());
        assertEquals(0x05, PersonalityFactor.NEUTRAL.targetId());
        assertEquals(4, Mode.values().length);

        WwwConfig.ActionDistance config = WwwConfigLoader.loadDefaults().runtime().actionDistance();
        assertEquals(5.0, ActionDistance.NARROW.blocks(config));
        assertEquals(30.0, ActionDistance.VERY_WIDE.blocks(config));
    }
}
