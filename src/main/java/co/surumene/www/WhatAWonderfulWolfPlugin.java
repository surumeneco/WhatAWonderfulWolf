package co.surumene.www;

import co.surumene.wgl.plugin.WonderfulGenomeLibService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class WhatAWonderfulWolfPlugin extends JavaPlugin {
    private WonderfulGenomeLibService genomeLib;

    @Override
    public void onEnable() {
        genomeLib = Bukkit.getServicesManager().load(WonderfulGenomeLibService.class);
        if (genomeLib == null) {
            throw new IllegalStateException("WonderfulGenomeLib service is unavailable");
        }
    }

    public WonderfulGenomeLibService genomeLib() {
        if (genomeLib == null) {
            throw new IllegalStateException("WonderfulGenomeLib service is not available");
        }
        return genomeLib;
    }
}
