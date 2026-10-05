package co.surumene.www.persistence;

import co.surumene.www.domain.*;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfRuntimeCodecV1Test {
    private final WonderfulWolfRuntimeCodecV1 codec = new WonderfulWolfRuntimeCodecV1();

    @Test
    void roundTripsAllRuntimeAndPedigreeStateWithoutEmbeddingGenomeOrPhenotype() {
        WonderfulWolfIndividual source = individual(false);

        byte[] encoded = codec.encode(source);
        WonderfulWolfIndividual restored =
                codec.decode(encoded, source.genome(), source.phenotypeSnapshot());

        assertEquals(source, restored);
        assertSame(source.genome(), restored.genome());
        assertSame(source.phenotypeSnapshot(), restored.phenotypeSnapshot());
    }

    @Test
    void producesCanonicalBytesRegardlessOfMapInsertionOrder() {
        WonderfulWolfIndividual first = individual(false);
        WonderfulWolfIndividual reversed = individual(true);

        assertArrayEquals(codec.encode(first), codec.encode(reversed));
    }

    @Test
    void rejectsUnsupportedContainerVersionAndTrailingBytes() {
        byte[] encoded = codec.encode(individual(false));
        encoded[4] = 2;
        assertThrows(PersistenceCodecException.class, () ->
                codec.decode(encoded, genome(), phenotype()));

        byte[] valid = codec.encode(individual(false));
        assertThrows(PersistenceCodecException.class, () ->
                codec.decode(Arrays.copyOf(valid, valid.length + 1), genome(), phenotype()));
    }

    private static WonderfulWolfIndividual individual(boolean reverseMaps) {
        UUID owner = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID commander = UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID playerA = UUID.fromString("30000000-0000-0000-0000-000000000003");
        UUID playerB = UUID.fromString("40000000-0000-0000-0000-000000000004");
        UUID world = UUID.fromString("50000000-0000-0000-0000-000000000005");

        LinkedHashMap<UUID, Long> affection = new LinkedHashMap<>();
        LinkedHashMap<Integer, ItemStackSnapshot> inventory = new LinkedHashMap<>();
        if (reverseMaps) {
            affection.put(playerB, -998877L);
            affection.put(playerA, 123456L);
            inventory.put(44, new ItemStackSnapshot(new byte[]{9, 8, 7, 6}));
            inventory.put(0, new ItemStackSnapshot(new byte[]{1, 2, 3}));
        } else {
            affection.put(playerA, 123456L);
            affection.put(playerB, -998877L);
            inventory.put(0, new ItemStackSnapshot(new byte[]{1, 2, 3}));
            inventory.put(44, new ItemStackSnapshot(new byte[]{9, 8, 7, 6}));
        }

        ParentSnapshot parentA = new ParentSnapshot(
                new AncestorSnapshot("Alpha", 2, "01-02-03-04-05-06"),
                Personality.BRAVE.name(),
                List.of(Trait.WATCHMAN.name(), Trait.GUARDIAN.name()),
                true);
        ParentSnapshot parentB = new ParentSnapshot(
                new AncestorSnapshot("Beta", 2, "11-12-13-14-15-16"),
                Personality.GENTLE.name(),
                List.of(Trait.HEALING.name()),
                false);

        PedigreeSnapshot pedigree = new PedigreeSnapshot(
                Optional.of(parentA),
                Optional.of(parentB),
                Optional.of(new AncestorSnapshot("AA", 1, "21-22-23-24-25-26")),
                Optional.of(new AncestorSnapshot("AB", 1, "31-32-33-34-35-36")),
                Optional.of(new AncestorSnapshot("BA", 1, "41-42-43-44-45-46")),
                Optional.of(new AncestorSnapshot("BB", 1, "51-52-53-54-55-56")));

        return new WonderfulWolfIndividual(
                genome(),
                phenotype(),
                Optional.of(owner),
                987654321L,
                Mode.WAIT,
                Optional.of(commander),
                ActionDistance.VERY_WIDE,
                Optional.of(new WorldPosition(world, 12.25, -34.5, 678.75)),
                affection,
                Optional.of(new ItemStackSnapshot(new byte[]{5, 4, 3, 2, 1})),
                inventory,
                3,
                pedigree);
    }

    private static DiploidGenome genome() {
        BitSequence a = BitSequence.fromBits("1010101011110000");
        BitSequence b = BitSequence.fromBits("0101010100001111");
        return new DiploidGenome(1, List.of(new ChromosomePair(a, b)));
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
                new RelationshipPerformance(-5, 12),
                personality,
                Personality.SERIOUS,
                List.of(),
                development,
                List.of(),
                0.0,
                false);
    }
}
