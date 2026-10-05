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
        this(
                plugin,
                new WonderfulWolfPdcPersistence(
                        genomeEngine,
                        new PhenotypeSnapshotCodecV1(),
                        new WonderfulWolfRuntimeCodecV1()));
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
        return persistence.restore(values(wolf));
    }

    private PersistentValueStore values(Wolf wolf) {
        return new PaperPersistentValueStore(plugin, wolf.getPersistentDataContainer());
    }
}
