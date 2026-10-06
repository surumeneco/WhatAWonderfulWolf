package co.surumene.www.runtime;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.function.Supplier;

public final class WonderfulWolfTraitBootstrap {
    private WonderfulWolfTraitBootstrap() {}

    public static void install(
            JavaPlugin plugin,
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfAbilityRuntime abilities,
            WonderfulWolfBehaviorRuntime behavior,
            Supplier<WwwConfig.Runtime> config) {
        Objects.requireNonNull(plugin, "plugin");
        WonderfulWolfTraitRuntime runtime =
                new WonderfulWolfTraitRuntime(
                        loaded,
                        abilities,
                        behavior,
                        config,
                        Bukkit.getServer(),
                        plugin.getLogger());

        Bukkit.getPluginManager().registerEvents(
                new WonderfulWolfTraitListener(loaded, runtime),
                plugin);
        runtime.tick(Bukkit.getCurrentTick());
        Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> runtime.tick(Bukkit.getCurrentTick()),
                10L,
                10L);
    }
}
