package co.surumene.www;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.lifecycle.WglProfileRegistryGateway;
import co.surumene.www.lifecycle.WonderfulWolfProfileLifecycle;
import co.surumene.www.persistence.WonderfulWolfEntityStore;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.persistence.WonderfulWolfPersistenceListener;
import co.surumene.wgl.plugin.WonderfulGenomeLibService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;

public final class WhatAWonderfulWolfPlugin extends JavaPlugin {
    private WonderfulGenomeLibService genomeLib;
    private WonderfulWolfProfileLifecycle profileLifecycle;
    private WonderfulWolfLoadedIndividuals loadedIndividuals;

    @Override
    public void onEnable() {
        genomeLib = Bukkit.getServicesManager().load(WonderfulGenomeLibService.class);
        if (genomeLib == null) {
            throw new IllegalStateException("WonderfulGenomeLib service is unavailable");
        }

        saveDefaultConfig();
        WwwConfig config = WwwConfigLoader.load(getConfig());

        WonderfulWolfProfileLifecycle lifecycle =
                new WonderfulWolfProfileLifecycle(
                        new WglProfileRegistryGateway(genomeLib, this),
                        genomeLib.engine().geneSequenceCodec());
        lifecycle.start(config);
        profileLifecycle = lifecycle;

        WonderfulWolfEntityStore entityStore =
                new WonderfulWolfEntityStore(this, genomeLib.engine());
        WonderfulWolfLoadedIndividuals loaded =
                new WonderfulWolfLoadedIndividuals(entityStore);
        WonderfulWolfPersistenceListener persistenceListener =
                new WonderfulWolfPersistenceListener(loaded, getLogger());

        Bukkit.getPluginManager().registerEvents(persistenceListener, this);
        Bukkit.getWorlds().forEach(world ->
                persistenceListener.restoreAll(world.getEntitiesByClass(Wolf.class)));
        loadedIndividuals = loaded;
    }

    @Override
    public void onDisable() {
        WonderfulWolfLoadedIndividuals loaded = loadedIndividuals;
        loadedIndividuals = null;
        if (loaded != null) {
            loaded.clear();
        }

        WonderfulWolfProfileLifecycle lifecycle = profileLifecycle;
        profileLifecycle = null;
        if (lifecycle != null) {
            lifecycle.close();
        }
        genomeLib = null;
    }

    public void reloadWwwConfiguration() {
        reloadConfig();
        WwwConfig candidate = WwwConfigLoader.load(getConfig());
        lifecycle().reload(candidate);
    }

    public WwwConfig currentConfig() {
        return lifecycle().currentConfig();
    }

    public WonderfulGenomeLibService genomeLib() {
        WonderfulGenomeLibService service = genomeLib;
        if (service == null) {
            throw new IllegalStateException("WonderfulGenomeLib service is not available");
        }
        return service;
    }

    public WonderfulWolfLoadedIndividuals loadedIndividuals() {
        WonderfulWolfLoadedIndividuals loaded = loadedIndividuals;
        if (loaded == null) {
            throw new IllegalStateException("Wonderful Wolf persistence lifecycle is not available");
        }
        return loaded;
    }

    private WonderfulWolfProfileLifecycle lifecycle() {
        WonderfulWolfProfileLifecycle lifecycle = profileLifecycle;
        if (lifecycle == null) {
            throw new IllegalStateException("Wonderful Wolf profile lifecycle is not available");
        }
        return lifecycle;
    }
}
