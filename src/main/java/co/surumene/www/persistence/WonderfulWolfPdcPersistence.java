package co.surumene.www.persistence;

import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wgl.api.DiploidGenome;
import co.surumene.wgl.api.GenomeEngine;

import java.util.Objects;

public final class WonderfulWolfPdcPersistence {
    public static final String TYPE_KEY = "type";
    public static final String SCHEMA_VERSION_KEY = "schema_version";
    public static final String GENOME_KEY = "genome";
    public static final String PHENOTYPE_KEY = "phenotype_snapshot";
    public static final String RUNTIME_KEY = "runtime_state";

    public static final String MARKER_VALUE = "wonderful_wolf";
    public static final int SCHEMA_VERSION = 1;

    private final GenomeEngine genomeEngine;
    private final PhenotypeSnapshotCodecV1 phenotypeCodec;
    private final WonderfulWolfRuntimeCodecV1 runtimeCodec;

    public WonderfulWolfPdcPersistence(
            GenomeEngine genomeEngine,
            PhenotypeSnapshotCodecV1 phenotypeCodec,
            WonderfulWolfRuntimeCodecV1 runtimeCodec) {
        this.genomeEngine = Objects.requireNonNull(genomeEngine, "genomeEngine");
        this.phenotypeCodec = Objects.requireNonNull(phenotypeCodec, "phenotypeCodec");
        this.runtimeCodec = Objects.requireNonNull(runtimeCodec, "runtimeCodec");
    }

    public boolean isWonderful(PersistentValueStore store) {
        Objects.requireNonNull(store, "store");
        return MARKER_VALUE.equals(store.getString(TYPE_KEY));
    }

    public void save(PersistentValueStore store, WonderfulWolfIndividual individual) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(individual, "individual");

        byte[] genome = genomeEngine.encode(individual.genome());
        byte[] phenotype = phenotypeCodec.encode(individual.phenotypeSnapshot());
        byte[] runtime = runtimeCodec.encode(individual);

        /*
         * The marker is the commit point. Encoding finishes before the first mutation,
         * then an existing marker is removed so a partial write fails closed.
         */
        store.remove(TYPE_KEY);
        store.putBytes(GENOME_KEY, genome);
        store.putBytes(PHENOTYPE_KEY, phenotype);
        store.putBytes(RUNTIME_KEY, runtime);
        store.putInteger(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
        store.putString(TYPE_KEY, MARKER_VALUE);
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
        if (schemaVersion != SCHEMA_VERSION) {
            return failure(
                    RestoreFailureReason.UNSUPPORTED_SCHEMA,
                    "unsupported WWW PDC schema version: " + schemaVersion);
        }

        byte[] genomeBytes = requiredBytes(store, GENOME_KEY);
        byte[] phenotypeBytes = requiredBytes(store, PHENOTYPE_KEY);
        byte[] runtimeBytes = requiredBytes(store, RUNTIME_KEY);
        if (genomeBytes == null || phenotypeBytes == null || runtimeBytes == null) {
            return failure(RestoreFailureReason.MISSING_DATA, "missing required WWW PDC payload");
        }

        try {
            DiploidGenome genome = genomeEngine.decodeBinary(genomeBytes);
            PhenotypeSnapshot phenotype = phenotypeCodec.decode(phenotypeBytes);
            WonderfulWolfIndividual individual = runtimeCodec.decode(runtimeBytes, genome, phenotype);
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
