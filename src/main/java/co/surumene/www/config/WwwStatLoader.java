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
            double min = number(config, p + ".min", d.min());
            double max = number(config, p + ".max", d.max());
            Object mode = config.get(p + ".display");
            if (mode != null && !(mode instanceof String))
                throw new IllegalArgumentException(p + ".display must be text");
            String display = mode == null ? d.display() : (String) mode;
            result.put(ability, new WwwConfig.Stat(min, max, display));
        }
        return new WwwConfig.Stats(result);
    }
    private static double number(ConfigurationSection config, String path, double fallback) {
        Object value = config.get(path);
        if (value == null) return fallback;
        if (!(value instanceof Number n))
            throw new IllegalArgumentException(path + " must be numeric");
        return n.doubleValue();
    }
}
