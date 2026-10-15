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


    static boolean migrateJapaneseTraitDescriptions(YamlConfiguration user) {
        String[][] changes = {
            {"mining_support", "近くのプレイヤーの採掘速度を上げる。", "近くにいると仕事が早くなる。"},
            {"muscle", "近くのプレイヤーの攻撃力を上げる。", "近くにいると力が湧いてくる。"},
            {"guardian", "近くのプレイヤーに耐性を付与する。", "近くにいると勇気が湧いてくる。"},
            {"healing", "近くのプレイヤーに再生を付与する。", "近くにいると心が休まる。"},
            {"direct_inheritance", "繁殖時に親の能力Genome領域を優先して引き継ぐ。", "子供が能力を受け継ぎやすい。"},
            {"wild", "繁殖時の突然変異が起きやすくなる。", "思いがけない子供が生まれやすい。"},
            {"watchman", "周囲の危険なMobを検知して警告する。", "危険が近付くと教えてくれる。"},
            {"holy_poison", "攻撃した相手に毒を与え、アンデッドには聖なるダメージを与える。", "攻撃した相手をじわじわと弱らせる。"},
            {"quirk_boost", "性格による能力補正が強くなる。", "性格のクセが強くなる。"},
            {"unyielding", "自分の体力を少しずつ回復する。", "傷付いても少しずつ回復する。"},
            {"scavenger", "落ちているアイテムを回収し、負傷時は肉を食べて回復する。", "落ちているアイテムを拾うが、肉を食べてしまうことがある。"},
            {"life_drain", "Mobを倒したときに自分の体力を回復する。", "敵を倒すと生命力を奪う。"},
            {"fire_bird", "炎のダメージを受けない。", "炎の中でもへっちゃら。"},
            {"intimidation", "攻撃を当てた相手に弱体化を付与する。", "攻撃した相手が竦む。"},
            {"lightweight", "落下ダメージを受けない。", "高いところから落ちてもへっちゃら。"},
            {"target", "攻撃対象を発光させ、位置を見やすくする。", "狙っている敵を教えてくれる。"},
            {"swimmer", "水中で溺れない。", "水の中でもへっちゃら。"},
            {"snowborn", "凍結ダメージを受けない。", "冷たいところでもへっちゃら。"}
        };
        boolean changed = false;
        for (String[] row : changes) {
            String key = "trait." + row[0] + ".description";
            if (row[1].equals(user.getString(key))) {
                user.set(key, row[2]);
                changed = true;
            }
        }
        return changed;
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
            boolean changed = "lang/ja_jp.yml".equals(path) && migrateJapaneseTraitDescriptions(user);
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
