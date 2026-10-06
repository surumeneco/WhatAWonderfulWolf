package co.surumene.www.wolftrap;

import co.surumene.www.behavior.WonderfulWolfBehaviorRuntime;
import co.surumene.www.config.WwwConfig;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.spawn.PaperWonderfulWolfFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class WolfTrapBootstrap {
    private WolfTrapBootstrap() {}

    public static void install(
            JavaPlugin plugin,
            PaperWonderfulWolfFactory wolves,
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfBehaviorRuntime behavior,
            Supplier<WwwConfig.Runtime> config) {
        Objects.requireNonNull(plugin, "plugin");
        WolfTrapRuntime runtime = new WolfTrapRuntime(
                new WolfTrapStateStore(plugin),
                wolves,
                loaded,
                behavior,
                config,
                Bukkit.getServer(),
                () -> ThreadLocalRandom.current().nextDouble(),
                () -> ThreadLocalRandom.current().nextLong(),
                System::currentTimeMillis,
                plugin.getLogger());

        runtime.restoreLoaded();
        Bukkit.getPluginManager().registerEvents(
                new WolfTrapListener(runtime),
                plugin);
        Bukkit.getScheduler().runTaskTimer(
                plugin,
                runtime::tick,
                1L,
                1L);
    }
}
