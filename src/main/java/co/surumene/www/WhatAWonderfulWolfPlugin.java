package co.surumene.www;

import co.surumene.www.behavior.ManualTargetRegistry;
import co.surumene.www.behavior.PendingFeedTracker;
import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.behavior.WonderfulWolfCombatListener;
import co.surumene.www.behavior.WonderfulWolfCommandService;
import co.surumene.www.behavior.WonderfulWolfGoalAdapter;
import co.surumene.www.behavior.WonderfulWolfRelationshipListener;
import co.surumene.www.behavior.WonderfulWolfRelationshipRuntime;
import co.surumene.www.behavior.WonderfulWolfRelationshipService;
import co.surumene.www.breeding.WonderfulWolfBreedingListener;
import co.surumene.www.breeding.WonderfulWolfBreedingService;
import co.surumene.www.breeding.WonderfulWolfOffspringService;
import co.surumene.www.combat.WonderfulWolfWeaponRuntime;
import co.surumene.www.config.MessageService;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.config.WwwConfigLoader;
import co.surumene.www.lifecycle.WglProfileRegistryGateway;
import co.surumene.www.lifecycle.WonderfulWolfProfileLifecycle;
import co.surumene.www.persistence.WonderfulWolfEntityStore;
import co.surumene.www.persistence.WorldGenomePayloads;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.persistence.WonderfulWolfPersistenceListener;
import co.surumene.www.runtime.BiologicalClock;
import co.surumene.www.runtime.BiologicalClockListener;
import co.surumene.www.runtime.PaperAbilityProjector;
import co.surumene.www.runtime.WonderfulWolfAbilityRuntime;
import co.surumene.www.runtime.YamlBiologicalClockStateStore;
import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import co.surumene.www.spawn.AsyncWolfFounderCoordinator;
import co.surumene.www.spawn.WonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfNaturalSpawnListener;
import co.surumene.www.ui.WanWandListener;
import co.surumene.www.ui.WanWandService;
import co.surumene.www.ui.WonderfulWolfInventoryListener;
import co.surumene.www.ui.WonderfulWolfInventoryService;
import co.surumene.wgl.plugin.WonderfulGenomeLibService;
import co.surumene.wcl.api.runtime.ConsumerContext;
import co.surumene.wcl.plugin.WonderfulCoreLibraryService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public final class WhatAWonderfulWolfPlugin extends JavaPlugin {
    private WonderfulGenomeLibService genomeLib;
    private ConsumerContext coreContext;
    private MessageService messages;
    private WonderfulWolfProfileLifecycle profileLifecycle;
    private WonderfulWolfLoadedIndividuals loadedIndividuals;
    private BiologicalClock biologicalClock;
    private WonderfulWolfAbilityRuntime abilityRuntime;
    private WonderfulWolfBehaviorRuntime behaviorRuntime;
    private WonderfulWolfRelationshipRuntime relationshipRuntime;
    private WonderfulWolfCommandService commandService;
    private WonderfulWolfWeaponRuntime weaponRuntime;
    private WonderfulWolfInventoryService inventoryService;
    private WanWandService wanWandService;
    private BukkitTask abilityTask;
    private long abilityScheduledFor = Long.MAX_VALUE;
    private AsyncWolfFounderCoordinator asyncFounders;
    private BukkitTask behaviorTask;
    private BukkitTask traitTask;
    private long traitCadenceAnchor;
    private BukkitTask weaponTask;

    @Override
    public void onEnable() {
        genomeLib = Bukkit.getServicesManager().load(WonderfulGenomeLibService.class);
        if (genomeLib == null) {
            throw new IllegalStateException("WonderfulGenomeLib service is unavailable");
        }
        WonderfulCoreLibraryService core = Bukkit.getServicesManager().load(WonderfulCoreLibraryService.class);
        if (core == null) throw new IllegalStateException("WonderfulCoreLibrary service is unavailable");
        coreContext = core.openContext(this);

        saveDefaultConfig();
        messages = new MessageService(this);
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
                new WonderfulWolfEntityStore(this, genomeLib.engine(),
                        new WorldGenomePayloads(coreContext(), "www"));
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
        WonderfulWolfFactory founderFactory =
                new WonderfulWolfFactory(
                        genomeLib.engine(),
                        lifecycle::currentWolfProfile);
        PaperWonderfulWolfFactory paperFactory =
                new PaperWonderfulWolfFactory(
                        founderFactory,
                        loaded,
                        clock::currentTime,
                        abilities::initializeNew,
                        getLogger());
        asyncFounders = new AsyncWolfFounderCoordinator(this, getLogger());
        paperFactory.useAsyncCoordinator(asyncFounders);
        breedingListener.useAsyncCoordinator(asyncFounders);
        WonderfulWolfNaturalSpawnListener naturalSpawnListener =
                new WonderfulWolfNaturalSpawnListener(
                        paperFactory,
                        () -> lifecycle.currentConfig().runtime().spawn(),
                        () -> ThreadLocalRandom.current().nextDouble(),
                        () -> ThreadLocalRandom.current().nextLong(),
                        getLogger());

        ManualTargetRegistry manualTargets = new ManualTargetRegistry();
        WonderfulWolfRelationshipService relationshipService =
                new WonderfulWolfRelationshipService(loaded);
        PendingFeedTracker pendingFeeds = new PendingFeedTracker();
        pendingFeeds.onCleanupDue(delay ->
                Bukkit.getScheduler().runTaskLater(
                        this,
                        () -> pendingFeeds.cleanup(Bukkit.getCurrentTick()),
                        delay));
        WonderfulWolfBehaviorRuntime behavior =
                new WonderfulWolfBehaviorRuntime(
                        loaded,
                        abilities,
                        () -> lifecycle.currentConfig().runtime(),
                        manualTargets,
                        new WonderfulWolfGoalAdapter(Bukkit.getMobGoals()),
                        Bukkit.getServer(),
                        getLogger());
        WonderfulWolfCommandService commands =
                new WonderfulWolfCommandService(
                        loaded,
                        manualTargets,
                        behavior::onCommandStateChanged);
        co.surumene.www.wolftrap.WolfTrapBootstrap.install(
                this,
                paperFactory,
                loaded,
                behavior,
                () -> lifecycle.currentConfig().runtime());
        WonderfulWolfWeaponRuntime weapons =
                new WonderfulWolfWeaponRuntime(
                        loaded,
                        behavior,
                        getLogger());
        loaded.onWeaponSync(wolf -> {
            try {
                weapons.syncWeapon(wolf);
            } catch (RuntimeException error) {
                getLogger().warning("Failed to synchronize WWW weapon for "
                        + wolf.getUniqueId() + ": " + error.getMessage());
            }
        });
        loaded.onRegistered(behavior::onRegistered);
        loaded.onAbilityDirty(wolf -> {
            abilities.markDirty(wolf);
            scheduleAbilityTask();
        });
        loaded.onTraitWorkChanged(this::refreshTraitTimer);
        behavior.onTraitActivityChanged(this::refreshTraitTimer);
        loaded.onUnregister(id -> {
            weapons.untrack(id);
            behavior.untrack(id);
            abilities.untrack(id);
        });
        WanWandService wanWand = new WanWandService(this);
        wanWand.registerRecipe();
        WonderfulWolfInventoryService inventories =
                new WonderfulWolfInventoryService(
                        loaded,
                        abilities,
                        commands,
                        weapons,
                        genomeLib.engine(),
                        lifecycle::currentWolfProfile,
                        messages);
        inventories.setStatsSupplier(() -> lifecycle.currentConfig().runtime().stats());
        WonderfulWolfOffspringService offspringService =
                new WonderfulWolfOffspringService(
                        genomeLib.engine(),
                        lifecycle::currentWolfProfile);
        co.surumene.www.command.WonderfulWolfAdminCommands adminCommands =
                new co.surumene.www.command.WonderfulWolfAdminCommands(
                        this,
                        loaded,
                        abilities,
                        genomeLib.engine(),
                        paperFactory,
                        offspringService,
                        new co.surumene.www.command.WonderfulWolfAdminInfo(
                                Bukkit.getServer(),
                                genomeLib.engine(),
                                lifecycle::currentWolfProfile,
                                messages),
                        commands,
                        entityStore);
        getLifecycleManager().registerEventHandler(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> event.registrar().register(
                        adminCommands.build(),
                        "What a Wonderful Wolf administration",
                        java.util.List.of("whatawonderfulwolf")));

        WonderfulWolfRelationshipRuntime relationshipRuntime =
                new WonderfulWolfRelationshipRuntime(
                        loaded,
                        relationshipService,
                        () -> lifecycle.currentConfig().runtime().relationship(),
                        () -> ThreadLocalRandom.current().nextDouble(),
                        pendingFeeds,
                        getLogger());

        Bukkit.getPluginManager().registerEvents(persistenceListener, this);
        Bukkit.getPluginManager().registerEvents(breedingListener, this);
        Bukkit.getPluginManager().registerEvents(naturalSpawnListener, this);
        Bukkit.getPluginManager().registerEvents(
                new co.surumene.www.behavior.WonderfulWolfTameListener(this, loaded), this);
        Bukkit.getPluginManager().registerEvents(
                new WonderfulWolfRelationshipListener(
                        loaded,
                        relationshipService,
                        pendingFeeds,
                        () -> Bukkit.getCurrentTick(),
                        getLogger()),
                this);
        Bukkit.getPluginManager().registerEvents(
                new WonderfulWolfCombatListener(
                        loaded,
                        behavior,
                        () -> ThreadLocalRandom.current().nextDouble()),
                this);
        Bukkit.getPluginManager().registerEvents(
                new BiologicalClockListener(clock, getLogger()),
                this);
        Bukkit.getPluginManager().registerEvents(
                new WonderfulWolfInventoryListener(inventories),
                this);
        Bukkit.getPluginManager().registerEvents(
                new WanWandListener(
                        wanWand,
                        loaded,
                        inventories,
                        behavior,
                        () -> lifecycle.currentConfig().runtime(),
                        () -> ThreadLocalRandom.current().nextDouble()),
                this);

        Bukkit.getWorlds().forEach(world ->
                persistenceListener.restoreAll(world.getEntitiesByClass(Wolf.class)));
        naturalSpawnListener.resumeLoaded();

        this.relationshipRuntime = relationshipRuntime;
        loadedIndividuals = loaded;
        abilityRuntime = abilities;
        behaviorRuntime = behavior;
        commandService = commands;
        weaponRuntime = weapons;
        inventoryService = inventories;
        wanWandService = wanWand;

        abilities.tick();
        behavior.tick(Bukkit.getCurrentTick());
        weapons.tick(Bukkit.getCurrentTick());
        traitCadenceAnchor = Bukkit.getCurrentTick();

        scheduleAbilityTask();
        behaviorTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    long tick = Bukkit.getCurrentTick();
                    relationshipRuntime.tick(tick);
                    behavior.tick(tick);
                },
                5L,
                5L);
        refreshTraitTimer();
        weaponTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> weapons.tick(Bukkit.getCurrentTick()),
                1L,
                1L);
    }

    /** On-demand wakeup instead of a global empty ability tick every server tick. */
    private void scheduleAbilityTask() {
        WonderfulWolfAbilityRuntime abilities = abilityRuntime;
        if (abilities == null) return;
        long delay = abilities.nextWakeupDelay();
        if (delay == Long.MAX_VALUE) {
            if (abilityTask != null) abilityTask.cancel();
            abilityTask = null;
            abilityScheduledFor = Long.MAX_VALUE;
            return;
        }
        long due = Bukkit.getCurrentTick() + delay;
        if (abilityTask != null && abilityScheduledFor <= due) return;
        if (abilityTask != null) abilityTask.cancel();
        abilityScheduledFor = due;
        abilityTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            abilityTask = null;
            abilityScheduledFor = Long.MAX_VALUE;
            if (abilityRuntime == null) return;
            abilities.tick();
            scheduleAbilityTask();
        }, delay);
    }

    /** The 5-tick trait timer exists only while a relevant source or pulse is active. */
    private void refreshTraitTimer() {
        WonderfulWolfBehaviorRuntime behavior = behaviorRuntime;
        if (behavior == null) return;
        if (!behavior.hasTimedTraitWork()) {
            if (traitTask != null) {
                traitTask.cancel();
                traitTask = null;
            }
            return;
        }
        if (traitTask != null) return;
        long phase = Math.floorMod(Bukkit.getCurrentTick() - traitCadenceAnchor, 5L);
        long delay = phase == 0L ? 5L : 5L - phase;
        traitTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> behavior.tickTraits(Bukkit.getCurrentTick()),
                delay,
                5L);
    }

    @Override
    public void onDisable() {
        if (asyncFounders != null) asyncFounders.shutdown();
        WonderfulWolfInventoryService inventories = inventoryService;
        inventoryService = null;
        if (inventories != null) {
            inventories.closeAll();
        }

        WanWandService wanWand = wanWandService;
        wanWandService = null;
        if (wanWand != null) {
            wanWand.unregisterRecipe();
        }

        BukkitTask weaponScheduled = weaponTask;
        weaponTask = null;
        if (weaponScheduled != null) {
            weaponScheduled.cancel();
        }

        WonderfulWolfWeaponRuntime weapons = weaponRuntime;
        weaponRuntime = null;
        if (weapons != null) {
            weapons.clear();
        }

        BukkitTask behaviorScheduled = behaviorTask;
        behaviorTask = null;
        if (behaviorScheduled != null) {
            behaviorScheduled.cancel();
        }

        BukkitTask traitScheduled = traitTask;
        traitTask = null;
        if (traitScheduled != null) traitScheduled.cancel();

        relationshipRuntime = null;
        WonderfulWolfBehaviorRuntime behavior = behaviorRuntime;
        behaviorRuntime = null;
        commandService = null;
        if (behavior != null) {
            behavior.clear();
        }

        BukkitTask task = abilityTask;
        abilityTask = null;
        if (task != null) {
            task.cancel();
        }
        abilityScheduledFor = Long.MAX_VALUE;

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
        if (coreContext != null) {
            coreContext.close();
            coreContext = null;
        }
        genomeLib = null;
    }

    public void reloadWwwConfiguration() {
        reloadConfig();
        WwwConfig candidate = WwwConfigLoader.load(getConfig());
        World nextClockWorld =
                requireClockWorld(candidate.runtime().age().clockWorld());

        WonderfulWolfProfileLifecycle lifecycle = lifecycle();
        BiologicalClock clock = biologicalClock();
        WwwConfig previous = lifecycle.currentConfig();

        lifecycle.reload(candidate);
        try {
            if (!clock.worldName().equals(nextClockWorld.getName())) {
                clock.reconfigure(
                        nextClockWorld.getName(),
                        nextClockWorld::getFullTime);
            }
        } catch (RuntimeException clockError) {
            try {
                lifecycle.reload(previous);
            } catch (RuntimeException rollbackError) {
                clockError.addSuppressed(rollbackError);
            }
            throw clockError;
        }

        messages.reload();
        WonderfulWolfAbilityRuntime abilities = abilityRuntime;
        if (abilities != null) {
            abilities.markAllDirty();
            abilities.tick();
            scheduleAbilityTask();
        }
        WonderfulWolfRelationshipRuntime relationship = relationshipRuntime;
        if (relationship != null) {
            relationship.onConfigReload();
        }
        WonderfulWolfBehaviorRuntime behavior = behaviorRuntime;
        if (behavior != null) {
            behavior.tick(Bukkit.getCurrentTick());
            if (behavior.hasTimedTraitWork()) behavior.tickTraits(Bukkit.getCurrentTick());
            refreshTraitTimer();
        }
    }

    public WwwConfig currentConfig() {
        return lifecycle().currentConfig();
    }

    public ConsumerContext coreContext() {
        if (coreContext == null || coreContext.isClosed()) throw new IllegalStateException("WCL owner context is unavailable");
        return coreContext;
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

    public WonderfulWolfBehaviorRuntime behaviorRuntime() {
        WonderfulWolfBehaviorRuntime runtime = behaviorRuntime;
        if (runtime == null) {
            throw new IllegalStateException(
                    "Wonderful Wolf behavior runtime is not available");
        }
        return runtime;
    }

    public WonderfulWolfCommandService commandService() {
        WonderfulWolfCommandService service = commandService;
        if (service == null) {
            throw new IllegalStateException(
                    "Wonderful Wolf command service is not available");
        }
        return service;
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
