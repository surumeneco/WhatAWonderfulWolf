package co.surumene.www.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Locale-specific user messages with bundled defaults and non-destructive
 * upgrades of existing lang files, following WWC's language-file contract.
 */
public final class WonderfulWolfMessages {
    private final JavaPlugin plugin;
    private YamlConfiguration japanese;
    private YamlConfiguration english;

    public WonderfulWolfMessages(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        reload();
    }

    public void reload() {
        japanese = load("lang/ja_jp.yml");
        english = load("lang/en_us.yml");
    }

    public String text(CommandSender sender, String key, Object... args) {
        return text(sender instanceof Player player ? player.locale() : Locale.JAPANESE, key, args);
    }

    public String text(Locale locale, String key, Object... args) {
        YamlConfiguration translations = locale != null
                && locale.getLanguage().equalsIgnoreCase("ja")
                ? japanese : english;
        String result = translations.getString(key, key);
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
    }

    private YamlConfiguration load(String resource) {
        File output = new File(plugin.getDataFolder(), resource);
        if (!output.exists()) {
            plugin.saveResource(resource, false);
        }
        YamlConfiguration user = YamlConfiguration.loadConfiguration(output);
        try (var stream = plugin.getResource(resource)) {
            if (stream == null) throw new IllegalStateException("Missing bundled " + resource);
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            int added = 0;
            for (String key : bundled.getKeys(true)) {
                if (bundled.isConfigurationSection(key)
                        || user.isSet(key) || hasScalarAncestor(user, key)) continue;
                user.set(key, bundled.get(key));
                added++;
            }
            if (added > 0) {
                user.save(output);
                plugin.getLogger().info("Added " + added + " translations to " + resource);
            }
            user.setDefaults(bundled);
        } catch (Exception error) {
            plugin.getLogger().warning("Could not synchronize " + resource + ": "
                    + error.getMessage());
        }
        return user;
    }

    private static boolean hasScalarAncestor(ConfigurationSection data, String path) {
        int separator = path.indexOf('.');
        while (separator >= 0) {
            String ancestor = path.substring(0, separator);
            if (data.isSet(ancestor) && !data.isConfigurationSection(ancestor)) return true;
            separator = path.indexOf('.', separator + 1);
        }
        return false;
    }
}
