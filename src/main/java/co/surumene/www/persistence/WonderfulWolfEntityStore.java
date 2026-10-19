package co.surumene.www.persistence;

import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.wgl.api.GenomeEngine;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class WonderfulWolfEntityStore {
    private final Plugin plugin;
    private final WonderfulWolfPdcPersistence persistence;

    public WonderfulWolfEntityStore(Plugin plugin, GenomeEngine genomeEngine) {
        this(plugin, genomeEngine, (GenomePayloadFiles) null);
    }

    public WonderfulWolfEntityStore(
            Plugin plugin, GenomeEngine genomeEngine, GenomePayloadFiles files) {
        this(plugin, new WonderfulWolfPdcPersistence(
                genomeEngine,
                new PhenotypeSnapshotCodecV1(),
                new WonderfulWolfRuntimeCodecV1(), files));
    }

    public WonderfulWolfEntityStore(Plugin plugin, GenomeEngine engine, WorldGenomePayloads records) {
        this(plugin, new WonderfulWolfPdcPersistence(engine,
                new PhenotypeSnapshotCodecV1(), new WonderfulWolfRuntimeCodecV1(), records));
    }

    public boolean migrateWorldRecord(Wolf wolf, boolean dryRun) {
        if (!wolf.isValid() || !isWonderful(wolf)) return false;
        return persistence.migrateExternalRecord(values(wolf), dryRun);
    }

    WonderfulWolfEntityStore(Plugin plugin, WonderfulWolfPdcPersistence persistence) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.persistence = Objects.requireNonNull(persistence, "persistence");
    }

    public boolean isWonderful(Entity entity) {
        return entity instanceof Wolf wolf
                && persistence.isWonderful(values(wolf));
    }

    public void save(Wolf wolf, WonderfulWolfIndividual individual) {
        Objects.requireNonNull(wolf, "wolf");
        persistence.save(values(wolf), individual);
    }

    public RestoreResult restore(Entity entity) {
        if (!(entity instanceof Wolf wolf)) {
            return new RestoreResult.NotWonderful();
        }
        PersistentValueStore values = values(wolf);
        RestoreResult result = persistence.restore(values);
        if (result instanceof RestoreResult.Success success
                && persistence.needsExternalMigration(values)) {
            try {
                // Inline Genome data is not removed until the new file is durable.
                persistence.save(values, success.individual());
            } catch (RuntimeException failure) {
                // Keep the intact old PDC readable; never discard the individual.
                if (plugin.getLogger() != null) {
                    plugin.getLogger().warning("Deferred WWW external Genome migration for "
                            + wolf.getUniqueId() + ": " + failure.getMessage());
                }
            }
        }
        return result;
    }

    private PersistentValueStore values(Wolf wolf) {
        return new PaperPersistentValueStore(plugin, wolf.getPersistentDataContainer(), wolf.getWorld());
    }
}
