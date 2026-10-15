package co.surumene.www.config;

import co.surumene.www.ability.AbilityRank;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.Personality;
import co.surumene.www.domain.Trait;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MessageBundleTest {
    @Test
    void japaneseAndEnglishResourcesCoverAllPlayerFacingGeneticLabels() throws Exception {
        for (String language : new String[]{"ja_jp", "en_us"}) {
            String file = "lang/" + language + ".yml";
            try (var stream = getClass().getClassLoader().getResourceAsStream(file)) {
                assertNotNull(stream, "Missing language bundle " + file);
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                for (Trait trait : Trait.values()) {
                    String prefix = "trait." + trait.name().toLowerCase(Locale.ROOT);
                    assertNotNull(yaml.getString(prefix + ".name"), file + " " + prefix);
                    assertNotNull(yaml.getString(prefix + ".description"), file + " " + prefix);
                }
                for (Ability ability : Ability.values()) {
                    String key = "ability." + ability.name().toLowerCase(Locale.ROOT);
                    assertNotNull(yaml.getString(key), file + " " + key);
                }
                for (Personality value : Personality.values()) {
                    String key = "personality." + value.name().toLowerCase(Locale.ROOT);
                    assertNotNull(yaml.getString(key), file + " " + key);
                }
                for (AbilityRank value : AbilityRank.values()) {
                    String key = "rank." + value.name().toLowerCase(Locale.ROOT);
                    assertNotNull(yaml.getString(key), file + " " + key);
                }
                for (Mode value : Mode.values()) {
                    String key = "mode." + value.name().toLowerCase(Locale.ROOT);
                    assertNotNull(yaml.getString(key), file + " " + key);
                }
                for (ActionDistance value : ActionDistance.values()) {
                    String key = "distance." + value.name().toLowerCase(Locale.ROOT);
                    assertNotNull(yaml.getString(key), file + " " + key);
                }
                for (String section : new String[]{"basic","pedigree","behavior","abilities",
                        "traits","age","injuries","relationship"}) {
                    assertNotNull(yaml.getString("info." + section), file + " info." + section);
                }
            }
        }
    }
}
