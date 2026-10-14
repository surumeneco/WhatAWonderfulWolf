package co.surumene.www.config;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * File-backed language bundles. Defaults are merged into the on-disk YAML
 * without overwriting custom or unknown user-defined keys.
 */
public final class MessageService {
    private final JavaPlugin plugin;
    private YamlConfiguration ja;
    private YamlConfiguration en;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ja = loadWithDefaults("lang/ja_jp.yml");
        en = loadWithDefaults("lang/en_us.yml");
    }

    public String text(CommandSender sender, String key, Object... args) {
        return text(sender instanceof Player player ? player.locale() : Locale.JAPANESE,
                key, args);
    }

    public String text(Locale locale, String key, Object... args) {
        String template = (locale != null
                && "ja".equalsIgnoreCase(locale.getLanguage()) ? ja : en)
                .getString(key, key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return template;
    }

    public String display(Locale locale, String category, Enum<?> value) {
        return text(locale, category + "." + value.name().toLowerCase(Locale.ROOT));
    }

    private YamlConfiguration loadWithDefaults(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
        YamlConfiguration user = YamlConfiguration.loadConfiguration(file);
        try (var stream = plugin.getResource(path)) {
            if (stream == null) throw new IllegalStateException("Missing bundled language: " + path);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key) || user.contains(key)) continue;
                user.set(key, defaults.get(key));
                changed = true;
            }
            if (changed) {
                user.save(file);
                plugin.getLogger().info("Added missing translations to " + path);
            }
            user.setDefaults(defaults);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not synchronize language file " + path + ": "
                    + e.getMessage());
            // Existing custom translations remain usable even if the file is read-only.
        }
        return user;
    }
}
