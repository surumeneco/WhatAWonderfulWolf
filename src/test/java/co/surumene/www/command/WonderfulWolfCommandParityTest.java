package co.surumene.www.command;

import co.surumene.www.ability.AbilityScale;
import co.surumene.www.domain.Ability;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfCommandParityTest {
    @Test
    void mapsAllTenWolfAbilitiesAndCanonicalValues() {
        for (Ability ability : Ability.values()) {
            assertEquals(0.0, WonderfulWolfCommandMutation.toNormalized(
                    ability, AbilityScale.toCanonical(ability, 0.0)), 0.02);
            assertEquals(1.0, WonderfulWolfCommandMutation.toNormalized(
                    ability, AbilityScale.toCanonical(ability, 1.0)), 0.02);
        }
        assertEquals(Ability.INVENTORY,
                WonderfulWolfCommandMutation.ability("inventory").orElseThrow());
        assertEquals(Ability.HEALTH,
                WonderfulWolfCommandMutation.ability("max-health").orElseThrow());
        assertTrue(WonderfulWolfCommandMutation.ability("unknown").isEmpty());
    }

    @Test
    void rejectsInvalidAdministrativeValues() {
        assertTrue(WonderfulWolfCommandMutation.toNormalized(Ability.HEALTH, 200.0) > 1.5);
        assertTrue(WonderfulWolfCommandMutation.toNormalized(Ability.SIZE, 0.75) < 0.0);
        assertEquals(200.0,
                WonderfulWolfCommandMutation.validateCanonical(Ability.HEALTH, 200.0));
        assertEquals(8.0,
                WonderfulWolfCommandMutation.validateCanonical(Ability.INVENTORY, 8.49));
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfCommandMutation.validateCanonical(Ability.HEALTH, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfCommandMutation.validateCanonical(Ability.DEFENSE, -1.0));
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfCommandMutation.toNormalized(Ability.SIZE, Double.NaN));
    }

    @Test
    void summonAcceptsDefaultRelativePositionAndCompound() {
        Location origin = new Location(null, 100, 64, 200);
        var parsed = WonderfulWolfSummonCommands.parse(origin,
                "~1 ~ ~-2 {stats:{size:2.5},baby:true}");
        assertEquals(101.0, parsed.location().getX());
        assertEquals(64.0, parsed.location().getY());
        assertEquals(198.0, parsed.location().getZ());
        assertNotNull(parsed.customData());
        assertNull(parsed.nbt());
        var map = new SnbtLikeParser(parsed.customData()).parseCompound();
        assertEquals(true, map.get("baby"));
        assertInstanceOf(Map.class, map.get("stats"));
    }

    @Test
    void summonDistinguishesVanillaNbtFromWolfData() {
        var p = WonderfulWolfSummonCommands.parse(new Location(null, 0, 0, 0),
                "{CustomName:'Wolf'}");
        assertNull(p.customData());
        assertEquals("{CustomName:'Wolf'}", p.nbt());
    }

    @Test
    void validatesConfigTypedValues() {
        assertEquals(3, WonderfulWolfConfigCommands.coerce("3", 1));
        assertEquals(0.5, WonderfulWolfConfigCommands.coerce("0.5", 0.0));
        assertEquals("world", WonderfulWolfConfigCommands.coerce("world", "other"));
        assertEquals(true, WonderfulWolfConfigCommands.coerce("true", false));
        assertThrows(IllegalArgumentException.class,
                () -> WonderfulWolfConfigCommands.coerce("maybe", false));
    }
}
