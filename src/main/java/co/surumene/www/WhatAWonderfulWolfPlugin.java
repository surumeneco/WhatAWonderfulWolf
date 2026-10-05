package co.surumene.www;

import co.surumene.www.breeding.WonderfulWolfBreedingListener;
import co.surumene.www.breeding.WonderfulWolfBreedingService;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.lifecycle.WglProfileRegistryGateway;
import co.surumene.www.lifecycle.WonderfulWolfProfileLifecycle;
import co.surumene.www.persistence.WonderfulWolfEntityStore;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.persistence.WonderfulWolfPersistenceListener;
import co.surumene.www.runtime.BiologicalClock;
import co.surumene.www.runtime.BiologicalClockListener;
import co.surumene.www.runtime.PaperAbilityProjector;
import co.surumene.www.runtime.WonderfulWolfAbilityRuntime;
import co.surumene.www.runtime.YamlBiologicalClockStateStore;
import co.surumene.wgl.plugin.WonderfulGenomeLibService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public final class WhatAWonderfulWolfPlugin extends JavaPlugin {
    private WonderfulGenomeLibService genomeLib;
    private WonderfulWolfProfileLifecycle profileLifecycle;
    private WonderfulWolfLoadedIndividuals loadedIndividuals;
    private BiologicalClock biologicalClock;
    private WonderfulWolfAbilityRuntime abilityRuntime;
    private BukkitTask abilityTask;

    @Override
    public void onEnable() {
        genomeLib = Bukkit.getServicesManager().load(WonderfulGenomeLibService.class);
        if (genomeLib == null) {
            throw new IllegalStateException("WonderfulGenomeLib service is unavailable");
        }

        saveDefaultConfig();
        WwwConfig config = WwwConfigLoader.load(getConfig());
        World clockWorld = requireClockWorld(config.runtime().age().clockWorld());

        WonderfulWolfProfileLifecycle lifecycle =
                new WonderfulWolfProfileLifecycle(
                        new WglProfileRegistryGateway(genomeLib, this),
                        genomeLib.engine().geneSequenceCodec());
        lifecycle.start(config);
        profileLifecycle = lifecycle;

        BiologicalClock clock = BiologicalClock.start(
                clockWorld.getName(),
                clockWorld::getFullTime,
                new YamlBiologicalClockStateStore(
                        new File(getDataFolder(), "biological-clock.yml"),
                        getLogger()));
        biologicalClock = clock;

        WonderfulWolfEntityStore entityStore =
                new WonderfulWolfEntityStore(this, genomeLib.engine());
        WonderfulWolfLoadedIndividuals loaded =
                new WonderfulWolfLoadedIndividuals(entityStore);
        WonderfulWolfPersistenceListener persistenceListener =
                new WonderfulWolfPersistenceListener(loaded, getLogger());
        WonderfulWolfBreedingService breedingService =
                new WonderfulWolfBreedingService(
                        genomeLib.engine(),
                        lifecycle::currentWolfProfile);
        WonderfulWolfBreedingListener breedingListener =
                new WonderfulWolfBreedingListener(
                        loaded,
                        breedingService,
                        () -> ThreadLocalRandom.current().nextLong(),
                        getLogger());
        WonderfulWolfAbilityRuntime abilities =
                new WonderfulWolfAbilityRuntime(
                        loaded,
                        clock,
                        () -> lifecycle.currentConfig().runtime(),
                        new PaperAbilityProjector(getLogger()),
                        getLogger());

        Bukkit.getPluginManager().registerEvents(persistenceListener, this);
        Bukkit.getPluginManager().registerEvents(breedingListener, this);
        Bukkit.getPluginManager().registerEvents(
                new BiologicalClockListener(clock),
                this);

        Bukkit.getWorlds().forEach(world ->
                persistenceListener.restoreAll(world.getEntitiesByClass(Wolf.class)));

        loadedIndividuals = loaded;
        abilityRuntime = abilities;
        abilities.tick();
        abilityTask = Bukkit.getScheduler().runTaskTimer(
                this,
                abilities::tick,
                20L,
                20L);
    }

    @Override
    public void onDisable() {
        BukkitTask task = abilityTask;
        abilityTask = null;
        if (task != null) {
            task.cancel();
        }

        WonderfulWolfAbilityRuntime abilities = abilityRuntime;
        abilityRuntime = null;
        if (abilities != null) {
            abilities.clear();
        }

        BiologicalClock clock = biologicalClock;
        biologicalClock = null;
        if (clock != null) {
            try {
                clock.persist();
            } catch (RuntimeException error) {
                getLogger().warning(
                        "Could not persist biological clock state: "
                                + error.getMessage());
            }
        }

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
        World nextClockWorld =
                requireClockWorld(candidate.runtime().age().clockWorld());

        lifecycle().reload(candidate);
        biologicalClock().reconfigure(
                nextClockWorld.getName(),
                nextClockWorld::getFullTime);

        WonderfulWolfAbilityRuntime abilities = abilityRuntime;
        if (abilities != null) {
            abilities.tick();
        }
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

    public WonderfulWolfAbilityRuntime abilityRuntime() {
        WonderfulWolfAbilityRuntime runtime = abilityRuntime;
        if (runtime == null) {
            throw new IllegalStateException("Wonderful Wolf ability runtime is not available");
        }
        return runtime;
    }

    public BiologicalClock biologicalClock() {
        BiologicalClock clock = biologicalClock;
        if (clock == null) {
            throw new IllegalStateException("Wonderful Wolf biological clock is not available");
        }
        return clock;
    }

    private World requireClockWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException(
                    "Configured biological clock world is unavailable: "
                            + worldName);
        }
        return world;
    }

    private WonderfulWolfProfileLifecycle lifecycle() {
        WonderfulWolfProfileLifecycle lifecycle = profileLifecycle;
        if (lifecycle == null) {
            throw new IllegalStateException("Wonderful Wolf profile lifecycle is not available");
        }
        return lifecycle;
    }
}
