package co.surumene.www.individual;

import co.surumene.www.domain.*;
import co.surumene.wgl.api.*;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfIndividualTest {
    @Test
    void snapshotsRuntimeStateIntoImmutableCollections() {
        UUID player = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Map<UUID, Long> affection = new java.util.HashMap<>(Map.of(player, 25L));
        Map<Integer, ItemStackSnapshot> inventory =
                new java.util.HashMap<>(Map.of(3, new ItemStackSnapshot(new byte[]{1, 2, 3})));

        WonderfulWolfIndividual individual = new WonderfulWolfIndividual(
                genome(),
                phenotype(),
                Optional.of(player),
                48000L,
                Mode.FOLLOW,
                Optional.of(player),
                ActionDistance.NORMAL,
                Optional.empty(),
                affection,
                Optional.empty(),
                inventory,
                0,
                PedigreeSnapshot.founder());

        affection.put(player, 99L);
        inventory.clear();

        assertEquals(25L, individual.affection().get(player));
        assertTrue(individual.inventory().containsKey(3));
        assertThrows(UnsupportedOperationException.class,
                () -> individual.affection().put(player, 10L));
        assertThrows(UnsupportedOperationException.class,
                () -> individual.inventory().clear());
    }

    @Test
    void commandAndWaitStateMustMatchMode() {
        UUID player = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertThrows(IllegalArgumentException.class, () -> new WonderfulWolfIndividual(
                genome(), phenotype(), Optional.empty(), 0L,
                Mode.WANDER, Optional.of(player), ActionDistance.NORMAL, Optional.empty(),
                Map.of(), Optional.empty(), Map.of(), 0, PedigreeSnapshot.founder()));

        assertThrows(IllegalArgumentException.class, () -> new WonderfulWolfIndividual(
                genome(), phenotype(), Optional.empty(), 0L,
                Mode.WAIT, Optional.of(player), ActionDistance.NORMAL, Optional.empty(),
                Map.of(), Optional.empty(), Map.of(), 0, PedigreeSnapshot.founder()));
    }

    @Test
    void affectionUsesTheSpecifiedAbsoluteRange() {
        UUID player = UUID.fromString("33333333-3333-3333-3333-333333333333");
        assertDoesNotThrow(() -> new WonderfulWolfIndividual(
                genome(), phenotype(), Optional.empty(), 0L,
                Mode.WANDER, Optional.empty(), ActionDistance.NORMAL, Optional.empty(),
                Map.of(player, WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION),
                Optional.empty(), Map.of(), 0, PedigreeSnapshot.founder()));

        assertThrows(IllegalArgumentException.class, () -> new WonderfulWolfIndividual(
                genome(), phenotype(), Optional.empty(), 0L,
                Mode.WANDER, Optional.empty(), ActionDistance.NORMAL, Optional.empty(),
                Map.of(player, WonderfulWolfIndividual.MAX_ABSOLUTE_AFFECTION + 1),
                Optional.empty(), Map.of(), 0, PedigreeSnapshot.founder()));
    }

    private static DiploidGenome genome() {
        BitSequence sequence = BitSequence.fromBits("10101010");
        return new DiploidGenome(1, List.of(new ChromosomePair(sequence, sequence)));
    }

    private static PhenotypeSnapshot phenotype() {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 0.5);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) personality.put(factor, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) development.put(factor, 0.5);
        DecoderIdentity identity = new DecoderIdentity(
                1,
                new byte[32],
                new ProfileDescriptor("wonderful-wolf", 1, new byte[32]));
        return new PhenotypeSnapshot(
                identity,
                abilities,
                new RelationshipPerformance(0, 10),
                personality,
                Personality.SERIOUS,
                List.of(),
                development,
                List.of(),
                0.0,
                false);
    }
}
