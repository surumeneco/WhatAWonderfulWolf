package co.surumene.www.config;
import co.surumene.www.domain.Ability;
import org.bukkit.configuration.ConfigurationSection;
import java.util.EnumMap;

public final class WwwStatLoader {
    private WwwStatLoader() {}
    public static WwwConfig.Stats load(ConfigurationSection config) {
        EnumMap<Ability, WwwConfig.Stat> result = new EnumMap<>(Ability.class);
        WwwConfig.Stats defaults = DefaultStats.create();
        for (Ability ability : Ability.values()) {
            String p = "stats." + DefaultStats.key(ability);
            WwwConfig.Stat d = defaults.get(ability);
            double min = config.getDouble(p + ".min", d.min());
            double max = config.getDouble(p + ".max", d.max());
            String display = config.getString(p + ".display", d.display());
            result.put(ability, new WwwConfig.Stat(min, max, display));
        }
        return new WwwConfig.Stats(result);
    }
}
