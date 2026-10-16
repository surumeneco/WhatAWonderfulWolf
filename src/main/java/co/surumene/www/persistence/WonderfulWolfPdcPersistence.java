package co.surumene.www.persistence;

import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wgl.api.DiploidGenome;
import co.surumene.wgl.api.GenomeEngine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WonderfulWolfPdcPersistence {
    public static final String TYPE_KEY = "type";
    public static final String SCHEMA_VERSION_KEY = "schema_version";
    public static final String GENOME_KEY = "genome";
    public static final String GENOME_REFERENCE_KEY = "genome_ref";
    public static final String PHENOTYPE_KEY = "phenotype_snapshot";
    public static final String RUNTIME_KEY = "runtime_state";

    public static final String MARKER_VALUE = "wonderful_wolf";
    public static final int SCHEMA_VERSION = 1;
    public static final int EXTERNAL_SCHEMA_VERSION = 2;

    private final GenomeEngine genomeEngine;
    private final PhenotypeSnapshotCodecV1 phenotypeCodec;
    private final WonderfulWolfRuntimeCodecV1 runtimeCodec;
    private final GenomePayloadFiles payloadFiles;

    /**
     * An in-process proof of which immutable objects were encoded (or validated
     * by restore) for each committed external payload reference. Entries are
     * deliberately bounded, so a cache miss falls back to the verified path.
     */
    private final Map<UUID, ImmutableRecord> recentImmutableRecords = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, ImmutableRecord> eldest) {
            return size() > 128;
        }
    };
    private record ImmutableRecord(DiploidGenome genome, PhenotypeSnapshot phenotype) {}

    public WonderfulWolfPdcPersistence(
            GenomeEngine genomeEngine,
            PhenotypeSnapshotCodecV1 phenotypeCodec,
            WonderfulWolfRuntimeCodecV1 runtimeCodec) {
        this(genomeEngine, phenotypeCodec, runtimeCodec, null);
    }

    public WonderfulWolfPdcPersistence(
            GenomeEngine genomeEngine,
            PhenotypeSnapshotCodecV1 phenotypeCodec,
            WonderfulWolfRuntimeCodecV1 runtimeCodec,
            GenomePayloadFiles payloadFiles) {
        this.genomeEngine = Objects.requireNonNull(genomeEngine, "genomeEngine");
        this.phenotypeCodec = Objects.requireNonNull(phenotypeCodec, "phenotypeCodec");
        this.runtimeCodec = Objects.requireNonNull(runtimeCodec, "runtimeCodec");
        this.payloadFiles = payloadFiles;
    }

    public boolean needsExternalMigration(PersistentValueStore store) {
        return payloadFiles != null && isWonderful(store)
                && Integer.valueOf(SCHEMA_VERSION).equals(store.getInteger(SCHEMA_VERSION_KEY));
    }

    public boolean isWonderful(PersistentValueStore store) {
        Objects.requireNonNull(store, "store");
        return MARKER_VALUE.equals(store.getString(TYPE_KEY));
    }

    public void save(PersistentValueStore store, WonderfulWolfIndividual individual) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(individual, "individual");

        // Runtime is always encoded. External immutable payloads are rewritten
        // only when we cannot prove that their existing reference already
        // belongs to the exact immutable Genome + Phenotype objects.
        byte[] runtime = runtimeCodec.encode(individual);
        byte[] genome = null;
        byte[] phenotype = null;
        UUID externalRef = null;
        if (payloadFiles != null) {
            String previous = store.getString(GENOME_REFERENCE_KEY);
            UUID previousRef = previous == null ? null : UUID.fromString(previous);
            ImmutableRecord known = previousRef == null ? null : recentImmutableRecords.get(previousRef);
            if (isWonderful(store)
                    && Integer.valueOf(EXTERNAL_SCHEMA_VERSION).equals(store.getInteger(SCHEMA_VERSION_KEY))
                    && known != null
                    && known.genome() == individual.genome()
                    && known.phenotype() == individual.phenotypeSnapshot()) {
                externalRef = previousRef;
            } else {
                genome = genomeEngine.encode(individual.genome());
                phenotype = phenotypeCodec.encode(individual.phenotypeSnapshot());
                // Publish before touching any PDC marker: failed writes retain
                // the previously committed reference and runtime payload.
                externalRef = payloadFiles.writeOrReuse(previousRef, genome, phenotype);
            }
        } else {
            genome = genomeEngine.encode(individual.genome());
            phenotype = phenotypeCodec.encode(individual.phenotypeSnapshot());
        }

        // The marker is the commit point; never expose partially changed state.
        store.remove(TYPE_KEY);
        if (externalRef == null) {
            store.putBytes(GENOME_KEY, genome);
            store.putBytes(PHENOTYPE_KEY, phenotype);
            store.remove(GENOME_REFERENCE_KEY);
            store.putInteger(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
        } else {
            store.putString(GENOME_REFERENCE_KEY, externalRef.toString());
            store.remove(GENOME_KEY);
            store.remove(PHENOTYPE_KEY);
            store.putInteger(SCHEMA_VERSION_KEY, EXTERNAL_SCHEMA_VERSION);
        }
        store.putBytes(RUNTIME_KEY, runtime);
        store.putString(TYPE_KEY, MARKER_VALUE);
        if (externalRef != null) {
            recentImmutableRecords.put(externalRef,
                    new ImmutableRecord(individual.genome(), individual.phenotypeSnapshot()));
        }
    }

    public RestoreResult restore(PersistentValueStore store) {
        Objects.requireNonNull(store, "store");
        if (!isWonderful(store)) {
            return new RestoreResult.NotWonderful();
        }

        Integer schemaVersion = store.getInteger(SCHEMA_VERSION_KEY);
        if (schemaVersion == null) {
            return failure(RestoreFailureReason.MISSING_DATA, "missing schema version");
        }
        if (schemaVersion != SCHEMA_VERSION && schemaVersion != EXTERNAL_SCHEMA_VERSION) {
            return failure(
                    RestoreFailureReason.UNSUPPORTED_SCHEMA,
                    "unsupported WWW PDC schema version: " + schemaVersion);
        }

        byte[] runtimeBytes = requiredBytes(store, RUNTIME_KEY);
        if (runtimeBytes == null) {
            return failure(RestoreFailureReason.MISSING_DATA, "missing WWW runtime payload");
        }

        try {
            byte[] genomeBytes;
            byte[] phenotypeBytes;
            if (schemaVersion == EXTERNAL_SCHEMA_VERSION) {
                String reference = store.getString(GENOME_REFERENCE_KEY);
                if (reference == null || payloadFiles == null) {
                    return failure(RestoreFailureReason.MISSING_DATA, "missing WWW external Genome reference");
                }
                GenomePayloadFiles.Payload payload = payloadFiles.read(UUID.fromString(reference));
                genomeBytes = payload.genome();
                phenotypeBytes = payload.phenotype();
            } else {
                genomeBytes = requiredBytes(store, GENOME_KEY);
                phenotypeBytes = requiredBytes(store, PHENOTYPE_KEY);
            }
            if (genomeBytes == null || phenotypeBytes == null) {
                return failure(RestoreFailureReason.MISSING_DATA, "missing required WWW Genome payload");
            }
            DiploidGenome genome = genomeEngine.decodeBinary(genomeBytes);
            PhenotypeSnapshot phenotype = phenotypeCodec.decode(phenotypeBytes);
            WonderfulWolfIndividual individual = runtimeCodec.decode(runtimeBytes, genome, phenotype);
            if (schemaVersion == EXTERNAL_SCHEMA_VERSION && payloadFiles != null) {
                UUID reference = UUID.fromString(store.getString(GENOME_REFERENCE_KEY));
                recentImmutableRecords.put(reference, new ImmutableRecord(genome, phenotype));
            }
            return new RestoreResult.Success(individual);
        } catch (RuntimeException error) {
            return failure(
                    RestoreFailureReason.CORRUPT_DATA,
                    error.getClass().getSimpleName() + ": " + safeMessage(error));
        }
    }

    private static byte[] requiredBytes(PersistentValueStore store, String key) {
        byte[] value = store.getBytes(key);
        return value == null || value.length == 0 ? null : value;
    }

    private static RestoreResult.Failure failure(RestoreFailureReason reason, String detail) {
        return new RestoreResult.Failure(reason, detail);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "decode failed" : message;
    }
}
