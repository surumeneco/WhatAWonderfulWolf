package co.surumene.www.persistence;

import co.surumene.www.domain.*;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;
import co.surumene.wgl.core.EngineConfig;
import co.surumene.wgl.core.WonderfulGenomeEngine;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

final class WonderfulWolfPdcPersistenceTest {
    private final WonderfulGenomeEngine engine =
            WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfPdcPersistence persistence =
            new WonderfulWolfPdcPersistence(
                    engine,
                    new PhenotypeSnapshotCodecV1(),
                    new WonderfulWolfRuntimeCodecV1());

    @Test
    void savesThreeIndependentPayloadsAndRestoresTheSameIndividual() {
        MemoryStore store = new MemoryStore();
        WonderfulWolfIndividual source = individual();

        persistence.save(store, source);

        assertEquals(WonderfulWolfPdcPersistence.MARKER_VALUE,
                store.strings.get(WonderfulWolfPdcPersistence.TYPE_KEY));
        assertEquals(WonderfulWolfPdcPersistence.SCHEMA_VERSION,
                store.integers.get(WonderfulWolfPdcPersistence.SCHEMA_VERSION_KEY));
        assertArrayEquals(engine.encode(source.genome()),
                store.bytes.get(WonderfulWolfPdcPersistence.GENOME_KEY));
        assertNotNull(store.bytes.get(WonderfulWolfPdcPersistence.PHENOTYPE_KEY));
        assertNotNull(store.bytes.get(WonderfulWolfPdcPersistence.RUNTIME_KEY));

        RestoreResult result = persistence.restore(store);
        RestoreResult.Success success = assertInstanceOf(RestoreResult.Success.class, result);
        assertEquals(source, success.individual());
    }

    @Test
    void entityWithoutMarkerIsNotWonderfulEvenIfStalePayloadsExist() {
        MemoryStore store = new MemoryStore();
        WonderfulWolfIndividual source = individual();
        persistence.save(store, source);
        store.remove(WonderfulWolfPdcPersistence.TYPE_KEY);

        assertInstanceOf(RestoreResult.NotWonderful.class, persistence.restore(store));
    }

    @Test
    void markerWithMissingOrCorruptRequiredDataFailsClosed() {
        MemoryStore missing = new MemoryStore();
        missing.putString(WonderfulWolfPdcPersistence.TYPE_KEY, WonderfulWolfPdcPersistence.MARKER_VALUE);
        missing.putInteger(WonderfulWolfPdcPersistence.SCHEMA_VERSION_KEY, WonderfulWolfPdcPersistence.SCHEMA_VERSION);

        RestoreResult.Failure missingFailure = assertInstanceOf(
                RestoreResult.Failure.class,
                persistence.restore(missing));
        assertEquals(RestoreFailureReason.MISSING_DATA, missingFailure.reason());

        MemoryStore corrupt = new MemoryStore();
        persistence.save(corrupt, individual());
        corrupt.putBytes(WonderfulWolfPdcPersistence.GENOME_KEY, new byte[]{1, 2, 3});

        RestoreResult.Failure corruptFailure = assertInstanceOf(
                RestoreResult.Failure.class,
                persistence.restore(corrupt));
        assertEquals(RestoreFailureReason.CORRUPT_DATA, corruptFailure.reason());
    }

    @Test
    void unsupportedSchemaVersionIsRejectedWithoutGuessing() {
        MemoryStore store = new MemoryStore();
        persistence.save(store, individual());
        store.putInteger(WonderfulWolfPdcPersistence.SCHEMA_VERSION_KEY, 99);

        RestoreResult.Failure failure = assertInstanceOf(
                RestoreResult.Failure.class,
                persistence.restore(store));

        assertEquals(RestoreFailureReason.UNSUPPORTED_SCHEMA, failure.reason());
    }

    @Test
    void markerIsCommittedLastAndIsRemovedBeforeAnUpdate() {
        RecordingStore store = new RecordingStore();
        persistence.save(store, individual());

        assertEquals("remove:" + WonderfulWolfPdcPersistence.TYPE_KEY, store.operations.getFirst());
        assertEquals(
                "string:" + WonderfulWolfPdcPersistence.TYPE_KEY,
                store.operations.getLast());
    }

    private static WonderfulWolfIndividual individual() {
        UUID owner = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID commander = UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID world = UUID.fromString("50000000-0000-0000-0000-000000000005");

        ParentSnapshot parentA = new ParentSnapshot(
                new AncestorSnapshot("Alpha", 1, "01-02-03-04-05-06"),
                Personality.BRAVE.name(),
                List.of(Trait.WATCHMAN.name()),
                true);

        return new WonderfulWolfIndividual(
                genome(),
                phenotype(),
                Optional.of(owner),
                123456L,
                Mode.WAIT,
                Optional.of(commander),
                ActionDistance.WIDE,
                Optional.of(new WorldPosition(world, 1.25, 64.0, -9.5)),
                Map.of(owner, 123L),
                Optional.of(new ItemStackSnapshot(new byte[]{9, 9, 9})),
                Map.of(0, new ItemStackSnapshot(new byte[]{1, 2, 3})),
                2,
                new PedigreeSnapshot(
                        Optional.of(parentA),
                        Optional.empty(),
                        Optional.of(new AncestorSnapshot("AA", 0, "11-12-13-14-15-16")),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));
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
                new RelationshipPerformance(0, 10),
                personality,
                Personality.SERIOUS,
                List.of(),
                development,
                List.of(),
                0.0,
                false);
    }

    private static class MemoryStore implements PersistentValueStore {
        final Map<String, String> strings = new HashMap<>();
        final Map<String, Integer> integers = new HashMap<>();
        final Map<String, byte[]> bytes = new HashMap<>();

        @Override public String getString(String key) { return strings.get(key); }
        @Override public Integer getInteger(String key) { return integers.get(key); }
        @Override public byte[] getBytes(String key) {
            byte[] value = bytes.get(key);
            return value == null ? null : value.clone();
        }
        @Override public void putString(String key, String value) { strings.put(key, value); }
        @Override public void putInteger(String key, int value) { integers.put(key, value); }
        @Override public void putBytes(String key, byte[] value) { bytes.put(key, value.clone()); }
        @Override public void remove(String key) {
            strings.remove(key);
            integers.remove(key);
            bytes.remove(key);
        }
    }

    private static final class RecordingStore extends MemoryStore {
        final List<String> operations = new ArrayList<>();

        @Override public void putString(String key, String value) {
            operations.add("string:" + key);
            super.putString(key, value);
        }
        @Override public void putInteger(String key, int value) {
            operations.add("integer:" + key);
            super.putInteger(key, value);
        }
        @Override public void putBytes(String key, byte[] value) {
            operations.add("bytes:" + key);
            super.putBytes(key, value);
        }
        @Override public void remove(String key) {
            operations.add("remove:" + key);
            super.remove(key);
        }
    }
}
