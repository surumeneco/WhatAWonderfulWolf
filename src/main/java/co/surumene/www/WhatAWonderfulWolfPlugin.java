package co.surumene.www;

import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.lifecycle.WglProfileRegistryGateway;
import co.surumene.www.lifecycle.WonderfulWolfProfileLifecycle;
import co.surumene.wgl.plugin.WonderfulGenomeLibService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class WhatAWonderfulWolfPlugin extends JavaPlugin {
    private WonderfulGenomeLibService genomeLib;
    private WonderfulWolfProfileLifecycle profileLifecycle;

    @Override
    public void onEnable() {
        genomeLib = Bukkit.getServicesManager().load(WonderfulGenomeLibService.class);
        if (genomeLib == null) {
            throw new IllegalStateException("WonderfulGenomeLib service is unavailable");
        }

        saveDefaultConfig();
        WwwConfig config = WwwConfigLoader.load(getConfig());

        WonderfulWolfProfileLifecycle lifecycle =
                new WonderfulWolfProfileLifecycle(new WglProfileRegistryGateway(genomeLib, this));
        lifecycle.start(config);
        profileLifecycle = lifecycle;
    }

    @Override
    public void onDisable() {
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

    private WonderfulWolfProfileLifecycle lifecycle() {
        WonderfulWolfProfileLifecycle lifecycle = profileLifecycle;
        if (lifecycle == null) {
            throw new IllegalStateException("Wonderful Wolf profile lifecycle is not available");
        }
        return lifecycle;
    }
}
