package co.surumene.www.translation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TraitDescriptionLocalizationTest {
    @Test
    void japaneseTraitDescriptionsMatchApprovedPlayerFacingCopy() throws Exception {
        var expected = Map.ofEntries(
                Map.entry("mining_support", "近くにいると仕事が早くなる。"),
                Map.entry("muscle", "近くにいると力が湧いてくる。"),
                Map.entry("guardian", "近くにいると勇気が湧いてくる。"),
                Map.entry("healing", "近くにいると心が休まる。"),
                Map.entry("direct_inheritance", "子供が能力を受け継ぎやすい。"),
                Map.entry("wild", "思いがけない子供が生まれやすい。"),
                Map.entry("watchman", "危険が近付くと教えてくれる。"),
                Map.entry("holy_poison", "攻撃した相手をじわじわと弱らせる。"),
                Map.entry("quirk_boost", "性格のクセが強くなる。"),
                Map.entry("unyielding", "傷付いても少しずつ回復する。"),
                Map.entry("scavenger", "落ちているアイテムを拾うが、肉を食べてしまうことがある。"),
                Map.entry("life_drain", "敵を倒すと生命力を奪う。"),
                Map.entry("fire_bird", "炎の中でもへっちゃら。"),
                Map.entry("intimidation", "攻撃した相手が竦む。"),
                Map.entry("lightweight", "高いところから落ちてもへっちゃら。"),
                Map.entry("target", "狙っている敵を教えてくれる。"),
                Map.entry("swimmer", "水の中でもへっちゃら。"),
                Map.entry("snowborn", "冷たいところでもへっちゃら。")
        );
        try (var stream = getClass().getClassLoader().getResourceAsStream("lang/ja_jp.yml")) {
            assertNotNull(stream);
            var yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertNotNull(yaml.getConfigurationSection("trait"));
            assertEquals(expected.size(), yaml.getConfigurationSection("trait").getKeys(false).size());
            expected.forEach((key, value) ->
                    assertEquals(value, yaml.getString("trait." + key + ".description"), key));
        }
    }
}
